package io.horizontalsystems.bankwallet.core.adapters.zcash

import android.content.Context
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.db.entity.isFailure
import cash.z.ecc.android.sdk.db.entity.isSubmitSuccess
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.fromHex
import cash.z.ecc.android.sdk.model.*
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.AddressType
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
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.math.max

@OptIn(ExperimentalCoroutinesApi::class)
class ZcashAdapter(
        context: Context,
        private val wallet: Wallet,
        restoreSettings: RestoreSettings,
        private val testMode: Boolean
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendZcashAdapter {

    private val confirmationsThreshold = 10
    private val decimalCount = 8
    private val network: ZcashNetwork = if (testMode) ZcashNetwork.Testnet else ZcashNetwork.Mainnet
    private val feeChangeHeight: Long = if (testMode) 1_028_500 else 1_077_550
    private val lightWalletEndpoint = LightWalletEndpoint.defaultForNetwork(network)

    private val synchronizer: Synchronizer
    private val transactionsProvider: ZcashTransactionsProvider

    private val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val accountType = (wallet.account.type as? AccountType.Mnemonic) ?: throw UnsupportedAccountException()
    private val seed = accountType.seed

    override val receiveAddress: String = runBlocking {
        DerivationTool.deriveShieldedAddress(seed, network)
    }

    init {
        val viewingKey = runBlocking {
            DerivationTool.deriveUnifiedViewingKeys(seed, network).first()
        }

        val initializerConfig = when (wallet.account.origin) {
            AccountOrigin.Created -> {
                Initializer.Config { config ->
                    config.newWallet(
                        viewingKey = viewingKey,
                        network = network,
                        lightWalletEndpoint = lightWalletEndpoint,
                        alias = getValidAliasFromAccountId(wallet.account.id)
                    )
                }
            }
            AccountOrigin.Restored -> {
                val birthdayHeight = restoreSettings.birthdayHeight?.let { height ->
                    max(network.saplingActivationHeight.value, height.toLong())
                }
                Initializer.Config { config ->
                    config.importWallet(
                        viewingKey = viewingKey,
                        birthday = birthdayHeight?.let { BlockHeight.new(network, it) },
                        network = network,
                        lightWalletEndpoint = lightWalletEndpoint,
                        alias = getValidAliasFromAccountId(wallet.account.id)
                    )
                }
            }
        }

        val initializer = Initializer.newBlocking(context, initializerConfig)

        synchronizer = Synchronizer.newBlocking(initializer)
        transactionsProvider = ZcashTransactionsProvider(receiveAddress)
        synchronizer.onProcessorErrorHandler = ::onProcessorError
        synchronizer.onChainErrorHandler = ::onChainError
    }

    private fun defaultFee(height: Long? = null): Zatoshi {
        val value = if (height == null || height > feeChangeHeight) 1_000L else 10_000L
        return Zatoshi(value)
    }

    private var syncState: AdapterState = AdapterState.Zcash(
        ZcashState.DownloadingBlocks(BlockProgress(null, null))
    )
        set(value) {
            if (value != field) {
                field = value
                adapterStateUpdatedSubject.onNext(Unit)
            }
        }

    @OptIn(FlowPreview::class)
    override fun start() {
        synchronizer.start()
        subscribe(synchronizer as SdkSynchronizer)
    }

    override fun stop() {
        synchronizer.stop()
    }

    override fun refresh() {
    }

    override val isMainnet = true

    override val debugInfo: String
        get() = ""

    override val balanceState: AdapterState
        get() = syncState

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceData: BalanceData
        get() = BalanceData(balance, balanceLocked)

    private val balance: BigDecimal
        get() {
            val walletBalance = synchronizer.saplingBalances.value ?: return BigDecimal.ZERO
            return walletBalance.available.convertZatoshiToZec(decimalCount)
        }

    private val balanceLocked: BigDecimal
        get() {
            val walletBalance = synchronizer.saplingBalances.value ?: return BigDecimal.ZERO
            return walletBalance.pending.convertZatoshiToZec(decimalCount)
        }

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val explorerTitle: String
        get() = "blockchair.com"

    override val transactionsState: AdapterState
        get() = syncState

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val lastBlockInfo: LastBlockInfo?
        get() = synchronizer.latestHeight?.value?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = lastBlockUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>> {
        val fromParams = from?.let {
            val transactionHash = it.transactionHash.fromHex().reversedArray()
            Triple(transactionHash, it.timestamp, it.transactionIndex)
        }

        return transactionsProvider.getTransactions(fromParams, transactionType, limit)
            .map { transactions ->
                transactions.map {
                    getTransactionRecord(it)
                }
            }
    }

    override fun getTransactionRecordsFlowable(token: Token?, transactionType: FilterTransactionType): Flowable<List<TransactionRecord>> {
        return transactionsProvider.getNewTransactionsFlowable(transactionType).map { transactions ->
            transactions.map { getTransactionRecord(it) }
        }
    }

    override fun getTransactionUrl(transactionHash: String): String? =
        if (testMode) null else "https://blockchair.com/zcash/transaction/$transactionHash"

    override val availableBalance: BigDecimal
        get() = synchronizer.saplingBalances.value
            ?.available
            ?.minus(defaultFee())
            .convertZatoshiToZec(decimalCount)
            .coerceAtLeast(BigDecimal.ZERO)

    override val fee: BigDecimal
        get() = defaultFee().convertZatoshiToZec(decimalCount)

    override suspend fun validate(address: String): ZCashAddressType {
        if (address == receiveAddress) throw ZcashError.SendToSelfNotAllowed
        return when (synchronizer.validateAddress(address)) {
            is AddressType.Invalid -> throw ZcashError.InvalidAddress
            is AddressType.Transparent -> ZCashAddressType.Transparent
            is AddressType.Shielded -> ZCashAddressType.Shielded
        }
    }

    @OptIn(FlowPreview::class)
    override fun send(amount: BigDecimal, address: String, memo: String, logger: AppLogger): Single<Unit> =
            Single.create { emitter ->
                try {
                    val spendingKey = runBlocking {
                            DerivationTool.deriveSpendingKeys(seed, network).first()
                        }
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

    // Subscribe to a synchronizer on its own scope and begin responding to events
    @OptIn(FlowPreview::class)
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
        synchronizer.status.collectWith(scope, ::onStatus)
        synchronizer.progress.distinctUntilChanged().collectWith(scope, ::onDownloadProgress)
        synchronizer.saplingBalances.collectWith(scope, ::onBalance)
        synchronizer.processorInfo.distinctUntilChanged().collectWith(scope, ::onProcessorInfo)
    }

    private fun onProcessorError(error: Throwable?): Boolean {
        error?.printStackTrace()
        return true
    }

    private fun onChainError(errorHeight: BlockHeight, rewindHeight: BlockHeight) {
    }

    private fun onStatus(status: Synchronizer.Status) {
        syncState = when (status) {
            Synchronizer.Status.STOPPED -> AdapterState.NotSynced(Exception("stopped"))
            Synchronizer.Status.DISCONNECTED -> AdapterState.NotSynced(Exception("disconnected"))
            Synchronizer.Status.DOWNLOADING -> AdapterState.Zcash(
                ZcashState.DownloadingBlocks(BlockProgress(null, null))
            )
            Synchronizer.Status.SCANNING -> AdapterState.Zcash(
                ZcashState.ScanningBlocks(BlockProgress(null, null))
            )
            Synchronizer.Status.SYNCED -> AdapterState.Synced
            else -> syncState
        }
    }

    private fun onDownloadProgress(progress: Int) {}

    private fun onProcessorInfo(processorInfo: CompactBlockProcessor.ProcessorInfo) {
        if (processorInfo.isDownloading){
            syncState = AdapterState.Zcash(
                ZcashState.DownloadingBlocks(
                    BlockProgress(
                        processorInfo.lastDownloadedHeight?.value,
                        processorInfo.networkBlockHeight?.value
                    )
                )
            )
        } else if(processorInfo.isScanning){
            syncState = AdapterState.Zcash(
                ZcashState.ScanningBlocks(
                    BlockProgress(
                        processorInfo.lastScannedHeight?.value,
                        processorInfo.networkBlockHeight?.value
                    )
                )
            )
        }

        lastBlockUpdatedSubject.onNext(Unit)
    }

    private fun onBalance(balance: WalletBalance?) {
        balanceUpdatedSubject.onNext(Unit)
    }

    private fun getTransactionRecord(transaction: ZcashTransaction): TransactionRecord {
        val transactionHashHex = transaction.transactionHash.toReversedHex()

        return if (transaction.isIncoming) {
            BitcoinIncomingTransactionRecord(
                token = wallet.token,
                uid = transactionHashHex,
                transactionHash = transactionHashHex,
                transactionIndex = transaction.transactionIndex,
                blockHeight = transaction.minedHeight.toInt(),
                confirmationsThreshold = confirmationsThreshold,
                timestamp = transaction.timestamp,
                fee = defaultFee(transaction.minedHeight).convertZatoshiToZec(decimalCount),
                failed = transaction.failed,
                lockInfo = null,
                conflictingHash = null,
                showRawTransaction = false,
                amount = Zatoshi(transaction.value).convertZatoshiToZec(decimalCount),
                from = null,
                memo = transaction.memo,
                source = wallet.transactionSource
            )
        } else {
            BitcoinOutgoingTransactionRecord(
                token = wallet.token,
                uid = transactionHashHex,
                transactionHash = transactionHashHex,
                transactionIndex = transaction.transactionIndex,
                blockHeight = transaction.minedHeight.toInt(),
                confirmationsThreshold = confirmationsThreshold,
                timestamp = transaction.timestamp,
                fee = defaultFee(transaction.minedHeight).convertZatoshiToZec(decimalCount),
                failed = transaction.failed,
                lockInfo = null,
                conflictingHash = null,
                showRawTransaction = false,
                amount = Zatoshi(transaction.value).convertZatoshiToZec(decimalCount).negate(),
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
        object SendToSelfNotAllowed : ZcashError()
    }

    sealed class ZcashState{
        class DownloadingBlocks(val blockProgress: BlockProgress): ZcashState()
        class ScanningBlocks(val blockProgress: BlockProgress): ZcashState()
    }

    class BlockProgress(val current: Long?, val total: Long?)

    companion object {
        private const val ALIAS_PREFIX = "zcash_"

        private fun getValidAliasFromAccountId(accountId: String): String {
            return ALIAS_PREFIX + accountId.replace("-", "_")
        }

        fun clear(accountId: String, testMode: Boolean) {
            val network = if (testMode) ZcashNetwork.Testnet else ZcashNetwork.Mainnet
            runBlocking {
                Initializer.erase(App.instance, network, getValidAliasFromAccountId(accountId))
            }
        }
    }
}
