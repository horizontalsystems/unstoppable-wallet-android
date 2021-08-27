package io.horizontalsystems.bankwallet.core.adapters.zcash

import android.content.Context
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.db.entity.isFailure
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.*
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.AddressType
import cash.z.ecc.android.sdk.type.WalletBalance
import cash.z.ecc.android.sdk.type.ZcashNetwork
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.lang.Integer.max
import java.math.BigDecimal
import java.util.*

class ZcashAdapter(
        context: Context,
        private val wallet: Wallet,
        restoreSettings: RestoreSettings,
        testMode: Boolean
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendZcashAdapter {

    private val confirmationsThreshold = 10
    private val network: ZcashNetwork = if (testMode) ZcashNetwork.Testnet else ZcashNetwork.Mainnet
    private val feeChangeHeight: Long = if (testMode) 1_028_500 else 1_077_550
    private val lightWalletDHost = if (testMode) network.defaultHost else "zcash.horizontalsystems.xyz"
    private val lightWalletDPort = 9067

    private val synchronizer: Synchronizer
    private val seed: ByteArray
    private val transactionsProvider: ZcashTransactionsProvider

    private val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private var scanProgress: Int = 0
    private var downloadProgress: Int = 0
    private val today: Date = Date()

    init {
        val accountType = (wallet.account.type as? AccountType.Mnemonic) ?: throw UnsupportedAccountException()
        seed = Mnemonic().toSeed(accountType.words)

        val isRestored = wallet.account.origin == AccountOrigin.Restored
        val birthdayHeight = when (wallet.account.origin) {
            AccountOrigin.Created -> null
            AccountOrigin.Restored -> restoreSettings.birthdayHeight?.let {
                max(network.saplingActivationHeight, it)
            }
        }

        val config = Initializer.Config { config ->
            config.setNetwork(network, lightWalletDHost, lightWalletDPort)
            config.setBirthdayHeight(birthdayHeight, isRestored)
            config.alias = getValidAliasFromAccountId(wallet.account.id)
            config.setSeed(seed, network)
        }

        transactionsProvider = ZcashTransactionsProvider()
        synchronizer = Synchronizer(Initializer(context, config))
        synchronizer.onProcessorErrorHandler = ::onProcessorError
        synchronizer.onChainErrorHandler = ::onChainError
    }

    private fun defaultFee(height: Long? = null): Long {
        return if (height == null || height > feeChangeHeight) 1_000 else 10_000
    }

    private var syncState: AdapterState = AdapterState.Syncing(0, null)
        set(value) {
            if (value != field) {
                field = value
                adapterStateUpdatedSubject.onNext(Unit)
            }
        }

    //region IAdapter
    override fun start() {
        synchronizer.start()
        subscribe(synchronizer as SdkSynchronizer)
    }

    override fun stop() {
        synchronizer.stop()
    }

    override fun refresh() {
    }

    override val debugInfo: String
        get() = ""
    //endregion

    //region IBalanceAdapter
    override val balanceState: AdapterState
        get() = syncState

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceData: BalanceData
        get() = BalanceData(balance, balanceLocked)

    private val balance: BigDecimal
        get() {
            val totalZatoshi = synchronizer.saplingBalances.value.availableZatoshi
            return if (totalZatoshi > 0)
                totalZatoshi.convertZatoshiToZec()
            else
                BigDecimal.ZERO
        }

    private val balanceLocked: BigDecimal
        get() {
            val latestBalance = synchronizer.saplingBalances.value
            val lockedBalance = synchronizer.saplingBalances.value.pendingZatoshi
            return if (lockedBalance > 0)
                lockedBalance.convertZatoshiToZec()
            else
                BigDecimal.ZERO
        }

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    //endregion

    //region IReceiveAdapter
    override val receiveAddress = DerivationTool.deriveShieldedAddress(seed, network)
    //endregion

    //region ITransactionsAdapter

    override val transactionsState: AdapterState
        get() = syncState

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val lastBlockInfo: LastBlockInfo?
        get() = LastBlockInfo(synchronizer.latestHeight)

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = lastBlockUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        coin: Coin?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>> {
        val fromParams = from?.let {
            val transactionHash = it.transactionHash.fromHex().reversedArray()
            Triple(transactionHash, it.timestamp, it.transactionIndex)
        }

        return transactionsProvider.getTransactions(fromParams, limit)
                .map { transactions ->
                    transactions.map {
                        getTransactionRecord(it)
                    }
                }
    }

    override fun getTransactionRecordsFlowable(coin: Coin?, transactionType: FilterTransactionType): Flowable<List<TransactionRecord>> {
        return transactionsProvider.newTransactionsFlowable.map { transactions ->
            transactions.map { getTransactionRecord(it) }
        }
    }
    //endregion

    //region ISendZcashAdapter
    override val availableBalance: BigDecimal
        get() = (synchronizer.saplingBalances.value.availableZatoshi - defaultFee()).coerceAtLeast(0).convertZatoshiToZec()

    override val fee: BigDecimal
        get() = defaultFee().convertZatoshiToZec()

    override fun validate(address: String): ZCashAddressType {
        return runBlocking {
            when (synchronizer.validateAddress(address)) {
                is AddressType.Invalid -> throw ZcashError.InvalidAddress
                is AddressType.Transparent -> ZCashAddressType.Transparent
                is AddressType.Shielded -> {
                    if (address == receiveAddress) {
                        throw ZcashError.SendToSelfNotAllowed
                    }
                    ZCashAddressType.Shielded
                }
            }
        }
    }

    override fun send(amount: BigDecimal, address: String, memo: String, logger: AppLogger): Single<Unit> =
            Single.create { emitter ->
                try {
                    val spendingKey = DerivationTool.deriveSpendingKeys(seed, network).first()
                    logger.info("call synchronizer.sendToAddress")
                    // use a scope that automatically cancels when the synchronizer stops
                    val scope = (synchronizer as SdkSynchronizer).coroutineScope
                    // don't return until the transaction creation is complete
                    synchronizer
                        .sendToAddress(spendingKey, amount.convertZecToZatoshi(), address, memo)
                        .filter { it.isSubmitSuccess() || it.isFailure() }
                        .take(1)
                        .onEach {
                            if (it.isSubmitSuccess()) {
                                emitter.onSuccess(Unit)
                            } else {
                                FailedTransaction(it.errorMessage).let { error ->
                                    logger.warning("send error", error)
                                    emitter.onError(error)
                                }
                            }
                        }
                        .catch {
                            logger.warning("send error", it)
                            emitter.onError(it)
                        }
                        .launchIn(scope)
                } catch (error: Throwable) {
                    logger.warning("send error", error)
                    emitter.onError(error)
                }
            }
    //endregion

    // Subscribe to a synchronizer on its own scope and begin responding to events
    private fun subscribe(synchronizer: SdkSynchronizer) {
        // Note: If any of these callback functions directly touch the UI, then the scope used here
        //       should not live longer than that UI or else the context and view tree will be
        //       invalid and lead to crashes. For now, we use a scope that is cancelled whenever
        //       synchronizer.stop is called.
        //       If the scope of the view is required for one of these, then consider using the
        //       related viewModelScope instead of the synchronizer's scope.
        //       synchronizer.coroutineScope cannot be accessed until the synchronizer is started
        val scope = synchronizer.coroutineScope
        synchronizer.clearedTransactions.distinctUntilChanged().collectWith(scope, transactionsProvider::onClearedTransactions)
        synchronizer.pendingTransactions.distinctUntilChanged().collectWith(scope, transactionsProvider::onPendingTransactions)
        synchronizer.status.distinctUntilChanged().collectWith(scope, ::onStatus)
        synchronizer.progress.distinctUntilChanged().collectWith(scope, ::onDownloadProgress)
        synchronizer.saplingBalances.collectWith(scope, ::onBalance)
        synchronizer.processorInfo.distinctUntilChanged().collectWith(scope, ::onProcessorInfo)
    }

    private fun onProcessorError(error: Throwable?): Boolean {
        error?.printStackTrace()
        return true
    }

    private fun onChainError(errorHeight: Int, rewindHeight: Int) {
    }

    private fun onStatus(status: Synchronizer.Status) {
        syncState = when (status) {
            Synchronizer.Status.STOPPED -> AdapterState.NotSynced(Exception("stopped"))
            Synchronizer.Status.DISCONNECTED -> AdapterState.NotSynced(Exception("disconnected"))
            Synchronizer.Status.SYNCED -> AdapterState.Synced
            else -> syncState
        }
    }

    private fun onDownloadProgress(progress: Int) {
        downloadProgress = progress
        updateSyncingState()
    }

    private fun onProcessorInfo(processorInfo: CompactBlockProcessor.ProcessorInfo) {
        scanProgress = processorInfo.scanProgress
        updateSyncingState()

        lastBlockUpdatedSubject.onNext(Unit)
    }

    private fun onBalance(balance: WalletBalance) {
        balanceUpdatedSubject.onNext(Unit)
    }

    private fun updateSyncingState() {
        val totalProgress = (downloadProgress + scanProgress) / 2

        if (totalProgress < 100) {
            // Workaround: progress doesn't show unless date exists
            // for Zcash, it's probably better to show progress percentage with a placeholder date
            // rather than no percentage, because scanning can take a long time
            syncState = AdapterState.Syncing(totalProgress, today)
        }
    }

    private fun getTransactionRecord(transaction: ZcashTransaction): TransactionRecord {
        val transactionHashHex = transaction.transactionHash.toHexReversed()
        var incoming = true

        if (!transaction.toAddress.isNullOrEmpty() && transaction.toAddress != receiveAddress) {
            incoming = false
        }

        return if (incoming) {
            BitcoinIncomingTransactionRecord(
                coin = wallet.coin,
                uid = transactionHashHex,
                transactionHash = transactionHashHex,
                transactionIndex = transaction.transactionIndex,
                blockHeight = transaction.minedHeight,
                confirmationsThreshold = confirmationsThreshold,
                timestamp = transaction.timestamp,
                fee = defaultFee(transaction.minedHeight.toLong()).convertZatoshiToZec(),
                failed = transaction.failed,
                lockInfo = null,
                conflictingHash = null,
                showRawTransaction = false,
                amount = transaction.value.convertZatoshiToZec(),
                from = null,
                memo = transaction.memo,
                source = wallet.transactionSource
            )
        } else {
            BitcoinOutgoingTransactionRecord(
                coin = wallet.coin,
                uid = transactionHashHex,
                transactionHash = transactionHashHex,
                transactionIndex = transaction.transactionIndex,
                blockHeight = transaction.minedHeight,
                confirmationsThreshold = confirmationsThreshold,
                timestamp = transaction.timestamp,
                fee = defaultFee(transaction.minedHeight.toLong()).convertZatoshiToZec(),
                failed = transaction.failed,
                lockInfo = null,
                conflictingHash = null,
                showRawTransaction = false,
                amount = transaction.value.convertZatoshiToZec(),
                to = transaction.toAddress,
                sentToSelf = false,
                memo = transaction.memo,
                source = wallet.transactionSource
            )
        }
    }

    enum class ZCashAddressType{
        Shielded, Transparent
    }

    sealed class ZcashError : Exception() {
        object InvalidAddress : ZcashError()
        object TransparentAddressNotAllowed : ZcashError()
        object SendToSelfNotAllowed : ZcashError()
    }

    companion object {
        private const val ALIAS_PREFIX = "zcash_"

        private fun getValidAliasFromAccountId(accountId: String): String {
            return ALIAS_PREFIX + accountId.replace("-", "_")
        }

        fun clear(accountId: String, testMode: Boolean) {
            val network = if (testMode) ZcashNetwork.Testnet else ZcashNetwork.Mainnet
            Initializer.erase(App.instance, network, getValidAliasFromAccountId(accountId))
        }
    }
}
