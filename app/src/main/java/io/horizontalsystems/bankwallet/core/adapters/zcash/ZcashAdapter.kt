package io.horizontalsystems.bankwallet.core.adapters.zcash

import android.content.Context
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.ext.*
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.validate.AddressType
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal

class ZcashAdapter(
        context: Context,
        wallet: Wallet,
        testMode: Boolean
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendZcashAdapter {

    private val confirmationsThreshold = 10
    private val feeChangeHeight: Long = if (testMode) 1_028_500 else 1_077_550
    private val lightWalletDHost = if (testMode) "lightwalletd.testnet.electriccoin.co" else "zcash.horizontalsystems.xyz"
    private val lightWalletDPort = 9067

    private val synchronizer: Synchronizer
    private val seed: ByteArray
    private val transactionsProvider: ZcashTransactionsProvider

    private val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private var scanProgress: Int = 0
    private var downloadProgress: Int = 0

    init {
        val accountType = (wallet.account.type as? AccountType.Zcash)
            ?: throw UnsupportedAccountException()
        val isRestored = wallet.account.origin == AccountOrigin.Restored
        seed = Mnemonic().toSeed(accountType.words)
        val config = Initializer.Config { config ->
            config.server(lightWalletDHost, lightWalletDPort)
            config.setBirthdayHeight(accountType.birthdayHeight?.toInt(), isRestored)
            config.alias = getValidAliasFromAccountId(wallet.account.id)
            config.setSeed(seed)
        }

        synchronizer = Synchronizer(Initializer(context, config))
        transactionsProvider = ZcashTransactionsProvider(synchronizer)

        synchronizer.status.distinctUntilChanged().collectWith(GlobalScope, ::onStatus)
        synchronizer.progress.distinctUntilChanged().collectWith(GlobalScope, ::onDownloadProgress)
        synchronizer.balances.distinctUntilChanged().collectWith(GlobalScope, ::onBalance)
        synchronizer.processorInfo.distinctUntilChanged().collectWith(GlobalScope, ::onProcessorInfo)

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

    override val balance: BigDecimal
        get() {
            val totalZatoshi = synchronizer.latestBalance.availableZatoshi
            return if (totalZatoshi > 0)
                totalZatoshi.convertZatoshiToZec()
            else
                BigDecimal.ZERO
        }

    override val balanceLocked: BigDecimal?
        get() {
            val latestBalance = synchronizer.latestBalance
            val lockedBalance = (latestBalance.totalZatoshi - latestBalance.availableZatoshi).coerceAtLeast(0)
            return if (lockedBalance > 0)
                lockedBalance.convertZatoshiToZec()
            else
                null
        }

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    //endregion

    //region IReceiveAdapter
    override val receiveAddress = DerivationTool.deriveShieldedAddress(seed)

    override fun getReceiveAddressType(wallet: Wallet): String? = null
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

    override fun getTransactions(from: TransactionRecord?, limit: Int): Single<List<TransactionRecord>> {
        val fromParams = from?.let {
            val transactionHash = it.transactionHash.fromHex().reversedArray()
            Triple(transactionHash, it.timestamp, it.transactionIndex)
        }

        return transactionsProvider.getTransactions(fromParams, limit)
                .map { transactions ->
                    transactions.mapNotNull {
                        getTransactionRecord(it)
                    }
                }
    }

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = transactionsProvider.newTransactionsFlowable.map { transactions -> transactions.mapNotNull { getTransactionRecord(it) } }
    //endregion

    //region ISendZcashAdapter
    override val availableBalance: BigDecimal
        get() = (synchronizer.latestBalance.availableZatoshi - defaultFee()).coerceAtLeast(0).convertZatoshiToZec()

    override val fee: BigDecimal
        get() = defaultFee().convertZatoshiToZec()

    override fun validate(address: String) {
        runBlocking {
            when (synchronizer.validateAddress(address)) {
                is AddressType.Invalid -> throw ZcashError.InvalidAddress
                is AddressType.Transparent -> throw ZcashError.TransparentAddressNotAllowed
                is AddressType.Shielded -> {
                    if (address == receiveAddress) {
                        throw ZcashError.SendToSelfNotAllowed
                    }
                }
            }
        }
    }

    override fun send(amount: BigDecimal, address: String, memo: String, logger: AppLogger): Single<Unit> =
            Single.create { emitter ->
                try {
                    val spendingKey = DerivationTool.deriveSpendingKeys(seed).first()
                    logger.info("call synchronizer.sendToAddress")
                    synchronizer.sendToAddress(spendingKey, amount.convertZecToZatoshi(), address, memo)
                            .collectWith(GlobalScope) {}
                    emitter.onSuccess(Unit)
                } catch (error: Throwable) {
                    logger.warning("send error", error)
                    emitter.onError(error)
                }
            }
    //endregion

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

    private fun onBalance(balance: CompactBlockProcessor.WalletBalance) {
        balanceUpdatedSubject.onNext(Unit)
    }

    private fun updateSyncingState() {
        val totalProgress = (downloadProgress + scanProgress) / 2

        if (totalProgress < 100) {
            syncState = AdapterState.Syncing(totalProgress, null)
        }
    }

    private fun getTransactionRecord(zcashTransaction: ZcashTransaction): TransactionRecord? =
            zcashTransaction.let {
                val transactionHashHex = it.transactionHash.toHexReversed()
                val type = when {
                    !it.toAddress.isNullOrEmpty() -> TransactionType.Outgoing
                    it.toAddress.isNullOrEmpty() && it.value >= 0L && it.minedHeight > 0 -> TransactionType.Incoming
                    else -> return null
                }

                TransactionRecord(
                        uid = transactionHashHex,
                        transactionHash = transactionHashHex,
                        transactionIndex = it.transactionIndex,
                        interTransactionIndex = 0,
                        type = type,
                        blockHeight = if (it.minedHeight > 0) it.minedHeight.toLong() else null,
                        confirmationsThreshold = confirmationsThreshold,
                        amount = it.value.convertZatoshiToZec(),
                        fee = defaultFee(it.minedHeight.toLong()).convertZatoshiToZec(),
                        timestamp = it.timestamp,
                        failed = it.failed,
                        from = null,
                        memo = it.memo,
                        to = it.toAddress,
                        lockInfo = null,
                        conflictingTxHash = null,
                        showRawTransaction = false
                )
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

        fun clear(accountId: String) {
            Initializer.erase(App.instance, getValidAliasFromAccountId(accountId))
        }
    }
}
