package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedFilterException
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionLockInfo
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.models.Address
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.models.TransactionFilterType
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.TransactionStatus
import io.horizontalsystems.bitcoincore.models.TransactionType
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.hodler.HodlerOutputData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date

abstract class BitcoinBaseAdapter(
    open val kit: AbstractKit,
    open val syncMode: BitcoinCore.SyncMode,
    backgroundManager: BackgroundManager,
    val wallet: Wallet,
    private val confirmationsThreshold: Int,
    protected val decimal: Int = 8
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

    override fun getTransactionRecordsFlowable(token: Token?, transactionType: FilterTransactionType): Flowable<List<TransactionRecord>> {
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
        token: Token?,
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
                val lastBlockDate = if (syncMode is BitcoinCore.SyncMode.Blockchair) null else kit.lastBlockInfo?.timestamp?.let { Date(it * 1000) }

                AdapterState.Syncing(progress, lastBlockDate)
            }
        }
    }

    fun send(
        amount: BigDecimal,
        address: String,
        feeRate: Int,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?,
        transactionSorting: TransactionDataSortMode?,
        rbfEnabled: Boolean,
        logger: AppLogger
    ): Single<Unit> {
        val sortingType = getTransactionSortingType(transactionSorting)
        return Single.create { emitter ->
            try {
                logger.info("call btc-kit.send")
                kit.send(
                    address = address,
                    value = (amount * satoshisInBitcoin).toLong(),
                    senderPay = true,
                    feeRate = feeRate,
                    sortType = sortingType,
                    unspentOutputs = unspentOutputs,
                    pluginData = pluginData ?: mapOf(),
                    rbfEnabled = rbfEnabled
                )
                emitter.onSuccess(Unit)
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    fun availableBalance(
        feeRate: Int,
        address: String?,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?
    ): BigDecimal {
        return try {
            val maximumSpendableValue = kit.maximumSpendableValue(address, feeRate, unspentOutputs, pluginData
                    ?: mapOf())
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
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData>?
    ): BitcoinFeeInfo? {
        return try {
            val satoshiAmount = (amount * satoshisInBitcoin).toLong()
            kit.sendInfo(
                value = satoshiAmount,
                address = address,
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

    fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val from = transaction.inputs.find { input ->
            input.address?.isNotBlank() == true
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
                        token = wallet.token,
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
                        token = wallet.token,
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
                        amount = satoshiToBTC(transaction.amount).negate(),
                        to = to,
                        sentToSelf = false
                )
            }
            TransactionType.SentToSelf -> {
                BitcoinOutgoingTransactionRecord(
                        source = wallet.transactionSource,
                        token = wallet.token,
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
                        amount = satoshiToBTC(transaction.amount).negate(),
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