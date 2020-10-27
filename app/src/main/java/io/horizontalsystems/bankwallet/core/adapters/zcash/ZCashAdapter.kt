package io.horizontalsystems.bankwallet.core.adapters.zcash

import android.content.Context
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.ext.*
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.validate.AddressType
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal

class ZCashAdapter(
        context: Context,
        wallet: Wallet,
        testMode: Boolean
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendZCashAdapter {

    private val confirmationsThreshold = 10
    private val feeInZatoshi = 10_000L //0.0001 ZEC

    private val synchronizer: Synchronizer
    private val seed: ByteArray
    private val transactionsProvider: ZCashTransactionsProvider

    private val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private var scanProgress: Int = 0
    private var downloadProgress: Int = 0

    init {
        val words = "".split(" ")
        seed = Mnemonic().toSeed(words)
        val initializer = Initializer(context) { builder ->
            val host = if (testMode) "lightwalletd.testnet.electriccoin.co" else "lightwalletd.electriccoin.co"
            builder.server(host, 9067)
            builder.setSeed(seed)
            builder.importedWalletBirthday(954_500) //TODO change birthday height
        }
        synchronizer = Synchronizer(initializer)
        transactionsProvider = ZCashTransactionsProvider(synchronizer)

        synchronizer.status.distinctUntilChanged().collectWith(GlobalScope, ::onStatus)
        synchronizer.progress.distinctUntilChanged().collectWith(GlobalScope, ::onDownloadProgress)
        synchronizer.balances.distinctUntilChanged().collectWith(GlobalScope, ::onBalance)
        synchronizer.processorInfo.distinctUntilChanged().collectWith(GlobalScope, ::onProcessorInfo)

        synchronizer.onProcessorErrorHandler = ::onProcessorError
        synchronizer.onChainErrorHandler = ::onChainError
    }

    //region IAdapter
    override fun start() {
        // TODO: reconsider coroutine scope
        synchronizer.start(GlobalScope)
    }

    override fun stop() {
        synchronizer.stop()
    }

    override fun refresh() {
    }

    override val debugInfo: String
        get() = ""//TODO("Not yet implemented")
    //endregion

    //region IBalanceAdapter
    override var state: AdapterState = AdapterState.Syncing(0, null)
        set(value) {
            if (value != field) {
                field = value
                adapterStateUpdatedSubject.onNext(Unit)
            }
        }

    override val stateUpdatedFlowable: Flowable<Unit>
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
                    transactions.map {
                        getTransactionRecord(it)
                    }
                }
    }

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = transactionsProvider.newTransactionsFlowable.map { transactions -> transactions.map { getTransactionRecord(it) } }
    //endregion

    //region ISendZCashAdapter
    override val availableBalance: BigDecimal
        get() = (synchronizer.latestBalance.availableZatoshi - feeInZatoshi).coerceAtLeast(0).convertZatoshiToZec()

    override val fee: BigDecimal = feeInZatoshi.convertZatoshiToZec()

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
        state = when (status) {
            Synchronizer.Status.STOPPED -> AdapterState.NotSynced(Exception("stopped"))
            Synchronizer.Status.DISCONNECTED -> AdapterState.NotSynced(Exception("disconnected"))
            Synchronizer.Status.SYNCED -> AdapterState.Synced
            else -> state
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
            state = AdapterState.Syncing(totalProgress, null)
        }
    }

    private fun getTransactionRecord(zCashTransaction: ZCashTransaction): TransactionRecord =
            zCashTransaction.let {
                val transactionHashHex = it.transactionHash.toHexReversed()
                val type = when {
                    !it.toAddress.isNullOrEmpty() -> TransactionType.Outgoing
                    it.toAddress.isNullOrEmpty() && it.value > 0L && it.minedHeight > 0 -> TransactionType.Incoming
                    else -> throw Exception("Unknown zcash transactions: $zCashTransaction")
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
                        fee = fee,
                        timestamp = it.timestamp,
                        failed = it.failed,
                        from = null,
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

}
