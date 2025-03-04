package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedFilterException
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionLockInfo
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.models.TransactionFilterType
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.TransactionStatus
import io.horizontalsystems.bitcoincore.models.TransactionType
import io.horizontalsystems.bitcoincore.rbf.ReplacementTransaction
import io.horizontalsystems.bitcoincore.rbf.ReplacementTransactionInfo
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.hodler.HodlerOutputData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date

abstract class BitcoinBaseAdapter(
    open val kit: AbstractKit,
    open val syncMode: BitcoinCore.SyncMode,
    private val backgroundManager: BackgroundManager,
    val wallet: Wallet,
    protected val decimal: Int = 8
) : IAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter, ISendBitcoinAdapter {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var transactionConfirmationsThreshold = 3

    abstract val satoshisInBitcoin: BigDecimal

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

    override val isMainNet: Boolean = true

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

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flowable<List<TransactionRecord>> = when (address) {
        null -> getTransactionRecordsFlowable(transactionType)
        else -> Flowable.empty()
    }

    private fun getTransactionRecordsFlowable(transactionType: FilterTransactionType): Flowable<List<TransactionRecord>> {
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
        get() = BalanceData(balance, balanceTimeLocked, balanceNotRelayed)

    private val balance: BigDecimal
        get() = satoshiToBTC(kit.balance.spendable)

    private val balanceTimeLocked: BigDecimal
        get() = satoshiToBTC(kit.balance.unspendableTimeLocked)

    private val balanceNotRelayed: BigDecimal
        get() = satoshiToBTC(kit.balance.unspendableNotRelayed)

    override fun start() {
        kit.start()
        subscribeToEvents()
    }

    override fun stop() {
        kit.stop()
        scope.cancel()
    }

    override fun refresh() {
        kit.refresh()
    }

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ) = when (address) {
        null -> getTransactionsAsync(from, limit, transactionType)
        else -> Single.just(listOf())
    }

    private fun getTransactionsAsync(
        from: TransactionRecord?,
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

    private fun subscribeToEvents() {
        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> {
                        kit.onEnterForeground()
                    }
                    BackgroundManagerState.EnterBackground -> {
                        kit.onEnterBackground()
                    }
                    BackgroundManagerState.AllActivitiesDestroyed -> {

                    }
                }
            }
        }
    }

    override fun getRawTransaction(transactionHash: String): String? {
        return kit.getRawTransaction(transactionHash)
    }

    fun speedUpTransactionInfo(transactionHash: String): ReplacementTransactionInfo? {
        return kit.speedUpTransactionInfo(transactionHash)
    }

    fun cancelTransactionInfo(transactionHash: String): ReplacementTransactionInfo? {
        return kit.cancelTransactionInfo(transactionHash)
    }

    fun speedUpTransaction(transactionHash: String, minFee: Long): Pair<ReplacementTransaction, BitcoinTransactionRecord> {
        val replacement = kit.speedUpTransaction(transactionHash, minFee)
        return Pair(replacement, transactionRecord(replacement.info))
    }

    fun cancelTransaction(transactionHash: String, minFee: Long): Pair<ReplacementTransaction, BitcoinTransactionRecord> {
        val replacement = kit.cancelTransaction(transactionHash, minFee)
        return Pair(replacement, transactionRecord(replacement.info))
    }

    fun send(replacementTransaction: ReplacementTransaction): FullTransaction {
        return kit.send(replacementTransaction)
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
                val lastBlockDate = if (syncMode is BitcoinCore.SyncMode.Blockchair) null else kit.lastBlockInfo?.timestamp?.let { Date(it * 1000) }

                AdapterState.Syncing(progress, lastBlockDate)
            }
        }
    }

    override fun send(
        amount: BigDecimal,
        address: String,
        memo: String?,
        feeRate: Int,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        transactionSorting: TransactionDataSortMode?,
        rbfEnabled: Boolean,
        logger: AppLogger
    ): BitcoinTransactionRecord? {
        val sortingType = getTransactionSortingType(transactionSorting)

        logger.info("call btc-kit.send")
        val fullTransaction = kit.send(
            address = address,
            memo = memo,
            value = (amount * satoshisInBitcoin).toLong(),
            senderPay = true,
            feeRate = feeRate,
            sortType = sortingType,
            unspentOutputs = unspentOutputs,
            pluginData = pluginData ?: mapOf(),
            rbfEnabled = rbfEnabled
        )

        val transaction = kit.getTransaction(fullTransaction.header.hash.toReversedHex())

//        val list = kit.transactions(limit = 1).blockingGet()
//        val transaction = list.first()

        return transaction?.let { transactionRecord(it) }
    }

    override fun availableBalance(
        feeRate: Int,
        address: String?,
        memo: String?,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?
    ): BigDecimal {
        return try {
            val maximumSpendableValue = kit.maximumSpendableValue(address, memo, feeRate, unspentOutputs, pluginData
                    ?: mapOf())
            satoshiToBTC(maximumSpendableValue, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    override fun minimumSendAmount(address: String?): BigDecimal? {
        return try {
            satoshiToBTC(kit.minimumSpendableValue(address).toLong(), RoundingMode.CEILING)
        } catch (e: Exception) {
            null
        }
    }

    override fun bitcoinFeeInfo(
        amount: BigDecimal,
        feeRate: Int,
        address: String?,
        memo: String?,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?
    ): BitcoinFeeInfo? {
        return try {
            val satoshiAmount = (amount * satoshisInBitcoin).toLong()
            kit.sendInfo(
                value = satoshiAmount,
                address = address,
                memo = memo,
                senderPay = true,
                feeRate = feeRate,
                unspentOutputs = unspentOutputs,
                pluginData = pluginData ?: mapOf()
            ).let {
                BitcoinFeeInfo(
                    unspentOutputs = it.unspentOutputs,
                    fee = satoshiToBTC(it.fee),
                    changeValue = satoshiToBTC(it.changeValue),
                    changeAddress = it.changeAddress
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun validate(address: String, pluginData: Map<Byte, IPluginData>?) {
        kit.validateAddress(address, pluginData ?: mapOf())
    }

    fun transactionRecord(transaction: TransactionInfo): BitcoinTransactionRecord {
        val from = transaction.inputs.find { input ->
            input.address?.isNotBlank() == true
        }?.address

        var transactionLockInfo: TransactionLockInfo? = null
        val lockedOutput = transaction.outputs.firstOrNull { it.pluginId == HodlerPlugin.id }
        if (lockedOutput != null) {
            val hodlerOutputData = lockedOutput.pluginData as? HodlerOutputData
            hodlerOutputData?.approxUnlockTime?.let { approxUnlockTime ->
                val lockedValueBTC = satoshiToBTC(lockedOutput.value)
                transactionLockInfo = TransactionLockInfo(
                    Date(approxUnlockTime * 1000),
                    hodlerOutputData.addressString,
                    lockedValueBTC,
                    hodlerOutputData.lockTimeInterval
                )
            }
        }
        val memo = transaction.outputs.firstOrNull { it.memo != null }?.memo

        return when (transaction.type) {
            TransactionType.Incoming -> {
                BitcoinIncomingTransactionRecord(
                        source = wallet.transactionSource,
                        token = wallet.token,
                        uid = transaction.uid,
                        transactionHash = transaction.transactionHash,
                        transactionIndex = transaction.transactionIndex,
                        blockHeight = transaction.blockHeight,
                        confirmationsThreshold = transactionConfirmationsThreshold,
                        timestamp = transaction.timestamp,
                        fee = satoshiToBTC(transaction.fee),
                        failed = transaction.status == TransactionStatus.INVALID,
                        lockInfo = transactionLockInfo,
                        conflictingHash = transaction.conflictingTxHash,
                        showRawTransaction = transaction.status == TransactionStatus.NEW || transaction.status == TransactionStatus.INVALID,
                        amount = satoshiToBTC(transaction.amount),
                        from = from,
                        memo = memo
                )
            }
            TransactionType.Outgoing -> {
                val to = transaction.outputs.find { output -> output.value > 0 && output.address != null && !output.mine }?.address
                BitcoinOutgoingTransactionRecord(
                    source = wallet.transactionSource,
                    token = wallet.token,
                    uid = transaction.uid,
                    transactionHash = transaction.transactionHash,
                    transactionIndex = transaction.transactionIndex,
                    blockHeight = transaction.blockHeight,
                    confirmationsThreshold = transactionConfirmationsThreshold,
                    timestamp = transaction.timestamp,
                    fee = satoshiToBTC(transaction.fee),
                    failed = transaction.status == TransactionStatus.INVALID,
                    lockInfo = transactionLockInfo,
                    conflictingHash = transaction.conflictingTxHash,
                    showRawTransaction = transaction.status == TransactionStatus.NEW || transaction.status == TransactionStatus.INVALID,
                    amount = satoshiToBTC(transaction.amount).negate(),
                    to = to,
                    sentToSelf = false,
                    memo = memo,
                    replaceable = transaction.rbfEnabled && transaction.blockHeight == null && transaction.conflictingTxHash == null
                )
            }
            TransactionType.SentToSelf -> {
                val to = transaction.outputs.firstOrNull { !it.changeOutput }?.address ?: transaction.outputs.firstOrNull()?.address
                BitcoinOutgoingTransactionRecord(
                    source = wallet.transactionSource,
                    token = wallet.token,
                    uid = transaction.uid,
                    transactionHash = transaction.transactionHash,
                    transactionIndex = transaction.transactionIndex,
                    blockHeight = transaction.blockHeight,
                    confirmationsThreshold = transactionConfirmationsThreshold,
                    timestamp = transaction.timestamp,
                    fee = satoshiToBTC(transaction.fee),
                    failed = transaction.status == TransactionStatus.INVALID,
                    lockInfo = transactionLockInfo,
                    conflictingHash = transaction.conflictingTxHash,
                    showRawTransaction = transaction.status == TransactionStatus.NEW || transaction.status == TransactionStatus.INVALID,
                    amount = satoshiToBTC(transaction.amount).negate(),
                    to = to,
                    sentToSelf = true,
                    memo = memo,
                    replaceable = transaction.rbfEnabled && transaction.blockHeight == null && transaction.conflictingTxHash == null
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
        fun getTransactionSortingType(sortType: TransactionDataSortMode?): TransactionDataSortType = when (sortType) {
            TransactionDataSortMode.Bip69 -> TransactionDataSortType.Bip69
            else -> TransactionDataSortType.Shuffle
        }
    }

}

data class BitcoinFeeInfo(
    val unspentOutputs: List<UnspentOutput>,
    val fee: BigDecimal,
    val changeValue: BigDecimal?,
    val changeAddress: Address?
)