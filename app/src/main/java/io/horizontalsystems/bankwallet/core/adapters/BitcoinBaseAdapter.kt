package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionLockInfo
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.hodler.HodlerOutputData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

abstract class BitcoinBaseAdapter(
    open val kit: AbstractKit,
    open val syncMode: SyncMode? = null,
    backgroundManager: BackgroundManager,
    val wallet: Wallet,
    protected val testMode: Boolean
) : IAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter, BackgroundManager.Listener {

    init {
        backgroundManager.registerListener(this)
    }

    abstract val satoshisInBitcoin: BigDecimal

    override fun willEnterForeground() {
        super.willEnterForeground()
        kit.onEnterForeground()
    }

    override fun didEnterBackground() {
        super.didEnterBackground()
        kit.onEnterBackground()
    }

    //
    // Adapter implementation
    //

    private var syncState: AdapterState = AdapterState.Syncing()
        set(value) {
            if (value != field) {
                field = value
                adapterStateUpdatedSubject.onNext(Unit)
            }
        }

    override val transactionsState
        get() = syncState

    override val balanceState
        get() = syncState

    override val lastBlockInfo: LastBlockInfo?
        get() = kit.lastBlockInfo?.let { LastBlockInfo(it.height, it.timestamp) }

    override val receiveAddress: String
        get() = kit.receiveAddress()

    protected val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val lastBlockUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    protected val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> = PublishSubject.create()

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = lastBlockUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun getTransactionRecordsFlowable(coin: PlatformCoin?, transactionType: FilterTransactionType): Flowable<List<TransactionRecord>> {
        val observable: Observable<List<TransactionRecord>> = when (transactionType) {
            FilterTransactionType.All -> {
                transactionRecordsSubject
            }
            FilterTransactionType.Incoming -> {
                transactionRecordsSubject
                    .map { records ->
                        records.filter {
                            it is BitcoinIncomingTransactionRecord ||
                                    (it is BitcoinOutgoingTransactionRecord && it.sentToSelf)
                        }
                    }
                    .filter {
                        it.isNotEmpty()
                    }
            }
            FilterTransactionType.Outgoing -> {
                transactionRecordsSubject
                    .map { records ->
                        records.filter { it is BitcoinOutgoingTransactionRecord }
                    }
                    .filter {
                        it.isNotEmpty()
                    }

            }
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> {
                Observable.empty()
            }
        }

        return observable.toFlowable(BackpressureStrategy.BUFFER)
    }

    override val debugInfo: String = ""

    override val balanceData: BalanceData
        get() = BalanceData(balance, balanceLocked)

    private val balance: BigDecimal
        get() = satoshiToBTC(kit.balance.spendable)

    private val balanceLocked: BigDecimal
        get() = satoshiToBTC(kit.balance.unspendable)

    override fun start() {
        kit.start()
    }

    override fun stop() {
        kit.stop()
    }

    override fun refresh() {
        kit.refresh()
    }

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        coin: PlatformCoin?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>> {
        return try {
            kit.transactions(from?.uid, getBitcoinTransactionTypeFilter(transactionType), limit).map { it.map { tx -> transactionRecord(tx) } }
        } catch (e: UnsupportedFilterException) {
            Single.just(listOf())
        }
    }

    private fun getBitcoinTransactionTypeFilter(transactionType: FilterTransactionType): TransactionFilterType? {
        return when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> TransactionFilterType.Incoming
            FilterTransactionType.Outgoing -> TransactionFilterType.Outgoing
            else -> throw UnsupportedFilterException()
        }
    }

    override fun getRawTransaction(transactionHash: String): String? {
        return kit.getRawTransaction(transactionHash)
    }

    protected fun setState(kitState: BitcoinCore.KitState) {
        syncState = when (kitState) {
            is BitcoinCore.KitState.Synced -> {
                AdapterState.Synced
            }
            is BitcoinCore.KitState.NotSynced -> {
                AdapterState.NotSynced(kitState.exception)
            }
            is BitcoinCore.KitState.ApiSyncing -> {
                AdapterState.SearchingTxs(kitState.transactions)
            }
            is BitcoinCore.KitState.Syncing -> {
                val progress = (kitState.progress * 100).toInt()
                val lastBlockDate = kit.lastBlockInfo?.timestamp?.let { Date(it * 1000) }

                AdapterState.Syncing(progress, lastBlockDate)
            }
        }
    }

    fun send(amount: BigDecimal, address: String, feeRate: Long, pluginData: Map<Byte, IPluginData>?, transactionSorting: TransactionDataSortingType?, logger: AppLogger): Single<Unit> {
        val sortingType = getTransactionSortingType(transactionSorting)
        return Single.create { emitter ->
            try {
                logger.info("call btc-kit.send")
                kit.send(address, (amount * satoshisInBitcoin).toLong(), true, feeRate.toInt(), sortingType, pluginData
                        ?: mapOf())
                emitter.onSuccess(Unit)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    fun availableBalance(feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal {
        return try {
            val maximumSpendableValue = kit.maximumSpendableValue(address, feeRate.toInt(), pluginData
                    ?: mapOf())
            satoshiToBTC(maximumSpendableValue, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun minimumSendAmount(address: String?): BigDecimal {
        return satoshiToBTC(kit.minimumSpendableValue(address).toLong(), RoundingMode.CEILING)
    }

    fun maximumSendAmount(pluginData: Map<Byte, IPluginData>): BigDecimal? {
        return kit.maximumSpendLimit(pluginData)?.let { maximumSpendLimit ->
            satoshiToBTC(maximumSpendLimit, RoundingMode.CEILING)
        }
    }

    fun fee(amount: BigDecimal, feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?): BigDecimal {
        return try {
            val satoshiAmount = (amount * satoshisInBitcoin).toLong()
            val fee = kit.fee(satoshiAmount, address, senderPay = true, feeRate = feeRate.toInt(), pluginData = pluginData
                    ?: mapOf())
            satoshiToBTC(fee, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun validate(address: String, pluginData: Map<Byte, IPluginData>?) {
        kit.validateAddress(address, pluginData ?: mapOf())
    }

    fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val from = transaction.inputs.find { input ->
            input.address != null
        }?.address

        val to = transaction.outputs.find { output ->
            output.value > 0 && output.address != null && !output.mine
        }?.address

        var transactionLockInfo: TransactionLockInfo? = null
        val lockedOutput = transaction.outputs.firstOrNull { it.pluginId == HodlerPlugin.id }
        if (lockedOutput != null) {
            val hodlerOutputData = lockedOutput.pluginData as? HodlerOutputData
            hodlerOutputData?.approxUnlockTime?.let { approxUnlockTime ->
                val lockedValueBTC = satoshiToBTC(lockedOutput.value)
                transactionLockInfo = TransactionLockInfo(Date(approxUnlockTime * 1000), hodlerOutputData.addressString, lockedValueBTC)
            }
        }

        return when (transaction.type) {
            TransactionType.Incoming -> {
                BitcoinIncomingTransactionRecord(
                        source = wallet.transactionSource,
                        coin = wallet.platformCoin,
                        uid = transaction.uid,
                        transactionHash = transaction.transactionHash,
                        transactionIndex = transaction.transactionIndex,
                        blockHeight = transaction.blockHeight,
                        confirmationsThreshold = confirmationsThreshold,
                        timestamp = transaction.timestamp,
                        fee = satoshiToBTC(transaction.fee),
                        failed = transaction.status == TransactionStatus.INVALID,
                        lockInfo = transactionLockInfo,
                        conflictingHash = transaction.conflictingTxHash,
                        showRawTransaction = transaction.status == TransactionStatus.NEW || transaction.status == TransactionStatus.INVALID,
                        amount = satoshiToBTC(transaction.amount),
                        from = from
                )
            }
            TransactionType.Outgoing -> {
                BitcoinOutgoingTransactionRecord(
                        source = wallet.transactionSource,
                        coin = wallet.platformCoin,
                        uid = transaction.uid,
                        transactionHash = transaction.transactionHash,
                        transactionIndex = transaction.transactionIndex,
                        blockHeight = transaction.blockHeight,
                        confirmationsThreshold = confirmationsThreshold,
                        timestamp = transaction.timestamp,
                        fee = satoshiToBTC(transaction.fee),
                        failed = transaction.status == TransactionStatus.INVALID,
                        lockInfo = transactionLockInfo,
                        conflictingHash = transaction.conflictingTxHash,
                        showRawTransaction = transaction.status == TransactionStatus.NEW || transaction.status == TransactionStatus.INVALID,
                        amount = satoshiToBTC(transaction.amount),
                        to = to,
                        sentToSelf = false
                )
            }
            TransactionType.SentToSelf -> {
                BitcoinOutgoingTransactionRecord(
                        source = wallet.transactionSource,
                        coin = wallet.platformCoin,
                        uid = transaction.uid,
                        transactionHash = transaction.transactionHash,
                        transactionIndex = transaction.transactionIndex,
                        blockHeight = transaction.blockHeight,
                        confirmationsThreshold = confirmationsThreshold,
                        timestamp = transaction.timestamp,
                        fee = satoshiToBTC(transaction.fee),
                        failed = transaction.status == TransactionStatus.INVALID,
                        lockInfo = transactionLockInfo,
                        conflictingHash = transaction.conflictingTxHash,
                        showRawTransaction = transaction.status == TransactionStatus.NEW || transaction.status == TransactionStatus.INVALID,
                        amount = satoshiToBTC(transaction.amount),
                        to = to,
                        sentToSelf = true
                )
            }
        }

    }

    val statusInfo: Map<String, Any>
        get() = kit.statusInfo()

    private fun satoshiToBTC(value: Long, roundingMode: RoundingMode = RoundingMode.HALF_EVEN): BigDecimal {
        return BigDecimal(value).divide(satoshisInBitcoin, decimal, roundingMode)
    }

    private fun satoshiToBTC(value: Long?): BigDecimal? {
        return satoshiToBTC(value ?: return null)
    }

    companion object {
        const val confirmationsThreshold = 3
        const val decimal = 8

        fun getTransactionSortingType(sortType: TransactionDataSortingType?): TransactionDataSortType = when (sortType) {
            TransactionDataSortingType.Bip69 -> TransactionDataSortType.Bip69
            else -> TransactionDataSortType.Shuffle
        }

        fun getBip(derivation: AccountType.Derivation): Bip = when (derivation) {
            AccountType.Derivation.bip44 -> Bip.BIP44
            AccountType.Derivation.bip49 -> Bip.BIP49
            AccountType.Derivation.bip84 -> Bip.BIP84
        }

        fun getSyncMode(mode: SyncMode?): BitcoinCore.SyncMode {
            return when (mode) {
                SyncMode.Slow -> BitcoinCore.SyncMode.Full()
                SyncMode.New -> BitcoinCore.SyncMode.NewWallet()
                SyncMode.Fast -> BitcoinCore.SyncMode.Api()
                null -> throw AdapterErrorWrongParameters("SyncMode is null")
            }
        }
    }

}
