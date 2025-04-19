package cash.p.terminal.core.adapters

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.UnsupportedFilterException
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.TransactionDataSortMode
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.modules.transactions.TransactionLockInfo
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IPluginData
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
import io.horizontalsystems.core.logger.AppLogger
import io.horizontalsystems.hodler.HodlerOutputData
import io.horizontalsystems.hodler.HodlerPlugin
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date

abstract class BitcoinBaseAdapter(
    open val kit: AbstractKit,
    open val syncMode: BitcoinCore.SyncMode,
    private val backgroundManager: BackgroundManager,
    val wallet: Wallet,
    private val confirmationsThreshold: Int,
    protected val decimal: Int = 8
) : IAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter {

    private val scope = CoroutineScope(Dispatchers.Default)

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
    protected val transactionRecordsSubject: PublishSubject<List<TransactionRecord>> =
        PublishSubject.create()

    override val balanceUpdatedFlow: Flow<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER).asFlow()

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = lastBlockUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER).asFlow()

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> = when (address) {
        null -> getTransactionRecordsFlowable(transactionType).asFlow()
        else -> emptyFlow()
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
                            it.transactionRecordType == TransactionRecordType.BITCOIN_INCOMING ||
                                    (it.transactionRecordType == TransactionRecordType.BITCOIN_OUTGOING &&
                                            (it as BitcoinTransactionRecord).sentToSelf)
                        }
                    }
                    .filter {
                        it.isNotEmpty()
                    }
            }

            FilterTransactionType.Outgoing -> {
                transactionRecordsSubject
                    .map { records ->
                        records.filter { it.transactionRecordType == TransactionRecordType.BITCOIN_OUTGOING }
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

    override suspend fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ) = when (address) {
        null -> getTransactionsAsync(from, limit, transactionType)
        else -> emptyList<TransactionRecord>()
    }

    private suspend fun getTransactionsAsync(
        from: TransactionRecord?,
        limit: Int,
        transactionType: FilterTransactionType
    ): List<TransactionRecord> {
        return try {
            kit.transactions(from?.uid, getBitcoinTransactionTypeFilter(transactionType), limit)
                .map { it.map { tx -> transactionRecord(tx) } }.await()
        } catch (e: UnsupportedFilterException) {
            emptyList<TransactionRecord>()
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

                    BackgroundManagerState.Unknown,
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

    fun speedUpTransaction(
        transactionHash: String,
        minFee: Long
    ): Pair<ReplacementTransaction, BitcoinTransactionRecord> {
        val replacement = kit.speedUpTransaction(transactionHash, minFee)
        return Pair(replacement, transactionRecord(replacement.info))
    }

    fun cancelTransaction(
        transactionHash: String,
        minFee: Long
    ): Pair<ReplacementTransaction, BitcoinTransactionRecord> {
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
                val lastBlockDate =
                    if (syncMode is BitcoinCore.SyncMode.Blockchair) null else kit.lastBlockInfo?.timestamp?.let {
                        Date(it * 1000)
                    }

                AdapterState.Syncing(progress, lastBlockDate)
            }
        }
    }

    fun send(
        amount: BigDecimal,
        address: String,
        memo: String?,
        feeRate: Int,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        transactionSorting: TransactionDataSortMode?,
        rbfEnabled: Boolean,
        logger: AppLogger
    ): Single<String> {
        val sortingType = getTransactionSortingType(transactionSorting)
        return Single.create { emitter ->
            try {
                logger.info("call btc-kit.send")
                val sendData = kit.send(
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
                emitter.onSuccess(sendData.header.uid)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    fun availableBalance(
        feeRate: Int,
        address: String?,
        memo: String?,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?
    ): BigDecimal {
        return try {
            val maximumSpendableValue = kit.maximumSpendableValue(
                address, memo, feeRate, unspentOutputs, pluginData
                    ?: mapOf()
            )
            satoshiToBTC(maximumSpendableValue, RoundingMode.CEILING)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun minimumSendAmount(address: String?): BigDecimal? {
        return try {
            satoshiToBTC(kit.minimumSpendableValue(address).toLong(), RoundingMode.CEILING)
        } catch (e: Exception) {
            null
        }
    }

    fun bitcoinFeeInfo(
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

    fun validate(address: String, pluginData: Map<Byte, IPluginData>?) {
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
                BitcoinTransactionRecord(
                    source = wallet.transactionSource,
                    token = wallet.token,
                    uid = transaction.uid,
                    transactionHash = transaction.transactionHash,
                    transactionIndex = transaction.transactionIndex,
                    blockHeight = transaction.blockHeight,
                    confirmationsThreshold = confirmationsThreshold,
                    timestamp = transaction.timestamp,
                    fee = satoshiToBTC(transaction.fee)?.let { TransactionValue.CoinValue(wallet.token, it) },
                    failed = transaction.status == TransactionStatus.INVALID,
                    lockInfo = transactionLockInfo,
                    conflictingHash = transaction.conflictingTxHash,
                    showRawTransaction = transaction.status == TransactionStatus.NEW || transaction.status == TransactionStatus.INVALID,
                    amount = satoshiToBTC(transaction.amount),
                    from = from,
                    to = null,
                    memo = memo,
                    transactionRecordType = TransactionRecordType.BITCOIN_INCOMING
                )
            }

            TransactionType.Outgoing -> {
                val to =
                    transaction.outputs.find { output -> output.value > 0 && output.address != null && !output.mine }?.address
                BitcoinTransactionRecord(
                    source = wallet.transactionSource,
                    token = wallet.token,
                    uid = transaction.uid,
                    transactionHash = transaction.transactionHash,
                    transactionIndex = transaction.transactionIndex,
                    blockHeight = transaction.blockHeight,
                    confirmationsThreshold = confirmationsThreshold,
                    timestamp = transaction.timestamp,
                    fee = satoshiToBTC(transaction.fee)?.let { TransactionValue.CoinValue(wallet.token, it) },
                    failed = transaction.status == TransactionStatus.INVALID,
                    lockInfo = transactionLockInfo,
                    conflictingHash = transaction.conflictingTxHash,
                    showRawTransaction = transaction.status == TransactionStatus.NEW || transaction.status == TransactionStatus.INVALID,
                    amount = satoshiToBTC(transaction.amount).negate(),
                    to = to,
                    from = null,
                    sentToSelf = false,
                    memo = memo,
                    replaceable = transaction.rbfEnabled && transaction.blockHeight == null && transaction.conflictingTxHash == null,
                    transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING
                )
            }

            TransactionType.SentToSelf -> {
                val to = transaction.outputs.firstOrNull { !it.changeOutput }?.address
                    ?: transaction.outputs.firstOrNull()?.address
                BitcoinTransactionRecord(
                    source = wallet.transactionSource,
                    token = wallet.token,
                    uid = transaction.uid,
                    transactionHash = transaction.transactionHash,
                    transactionIndex = transaction.transactionIndex,
                    blockHeight = transaction.blockHeight,
                    confirmationsThreshold = confirmationsThreshold,
                    timestamp = transaction.timestamp,
                    fee = satoshiToBTC(transaction.fee)?.let { TransactionValue.CoinValue(wallet.token, it) },
                    failed = transaction.status == TransactionStatus.INVALID,
                    lockInfo = transactionLockInfo,
                    conflictingHash = transaction.conflictingTxHash,
                    showRawTransaction = transaction.status == TransactionStatus.NEW || transaction.status == TransactionStatus.INVALID,
                    amount = satoshiToBTC(transaction.amount).negate(),
                    to = to,
                    from = null,
                    sentToSelf = true,
                    memo = memo,
                    replaceable = transaction.rbfEnabled && transaction.blockHeight == null && transaction.conflictingTxHash == null,
                    transactionRecordType = TransactionRecordType.BITCOIN_OUTGOING
                )
            }
        }

    }

    val statusInfo: Map<String, Any>
        get() = kit.statusInfo()

    private fun satoshiToBTC(
        value: Long,
        roundingMode: RoundingMode = RoundingMode.HALF_EVEN
    ): BigDecimal {
        return BigDecimal(value).divide(satoshisInBitcoin, decimal, roundingMode)
    }

    private fun satoshiToBTC(value: Long?): BigDecimal? {
        return satoshiToBTC(value ?: return null)
    }

    companion object {
        fun getTransactionSortingType(sortType: TransactionDataSortMode?): TransactionDataSortType =
            when (sortType) {
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