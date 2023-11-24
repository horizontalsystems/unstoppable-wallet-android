package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendTonAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.hdwalletkit.Curve
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tonkit.ConnectionManager
import io.horizontalsystems.tonkit.DriverFactory
import io.horizontalsystems.tonkit.SyncState
import io.horizontalsystems.tonkit.TonKit
import io.horizontalsystems.tonkit.TonKitFactory
import io.horizontalsystems.tonkit.TonTransaction
import io.horizontalsystems.tonkit.TransactionType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.rxSingle
import java.math.BigDecimal

class TonAdapter(
    private val wallet: Wallet,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter, ISendTonAdapter {

    private val decimals = 9

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val transactionsStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val tonKit: TonKit

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var balance = BigDecimal.ZERO

    init {
        val accountType = wallet.account.type
        if (accountType !is AccountType.Mnemonic) {
            throw UnsupportedAccountException()
        }

        val hdWallet = HDWallet(accountType.seed, 607, HDWallet.Purpose.BIP44, Curve.Ed25519)
        val privateKey = hdWallet.privateKey(0)
        tonKit = TonKitFactory(DriverFactory(App.instance), ConnectionManager(App.instance)).create(privateKey.privKeyBytes, wallet.account.id)
    }

    override fun start() {
        coroutineScope.launch {
            tonKit.balanceFlow.collect {
                balance = it.toBigDecimal().movePointLeft(decimals)
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            tonKit.balanceSyncStateFlow.collect {
                balanceState = convertToAdapterState(it)
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            tonKit.transactionsSyncStateFlow.collect {
                transactionsState = convertToAdapterState(it)
                transactionsStateUpdatedSubject.onNext(Unit)
            }
        }
        tonKit.start()
    }

    override fun stop() {
        tonKit.stop()
        coroutineScope.cancel()
    }

    override fun refresh() {
//        tonKit.refresh()
    }

    override val debugInfo: String
        get() = ""


    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val balanceData: BalanceData
        get() = BalanceData(balance)
    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val receiveAddress = tonKit.receiveAddress
    override val isMainNet = true

    override val explorerTitle = "tonscan.org"
    override var transactionsState: AdapterState = AdapterState.Syncing()
    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = transactionsStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val lastBlockInfo: LastBlockInfo?
        get() = null
    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = Flowable.empty()

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
    ) = rxSingle {
        val tonTransactionType = when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> TransactionType.Incoming
            FilterTransactionType.Outgoing -> TransactionType.Outgoing
            FilterTransactionType.Swap -> return@rxSingle listOf()
            FilterTransactionType.Approve -> return@rxSingle listOf()
        }

        tonKit
            .transactions(from?.transactionHash, tonTransactionType, limit.toLong())
            .map {
                createTransactionRecord(it)
            }
    }

    private fun createTransactionRecord(transaction: TonTransaction): TransactionRecord {
        val value = when (transaction.type) {
            TransactionType.Incoming.name -> transaction.value_?.toBigDecimal()?.movePointLeft(decimals)
            TransactionType.Outgoing.name -> transaction.value_?.toBigDecimal()?.movePointLeft(decimals)?.negate()
            else -> null
        } ?: BigDecimal.ZERO

        val fee = transaction.fee?.toBigDecimal()?.movePointLeft(decimals)

        val type = when (transaction.type) {
            TransactionType.Incoming.name -> TonTransactionRecord.Type.Incoming
            TransactionType.Outgoing.name -> TonTransactionRecord.Type.Outgoing
            else -> TonTransactionRecord.Type.Incoming
        }

        return TonTransactionRecord(
            uid = transaction.hash,
            transactionHash = transaction.hash,
            logicalTime = transaction.lt,
            blockHeight = null,
            confirmationsThreshold = null,
            timestamp = transaction.timestamp,
            source = wallet.transactionSource,
            mainValue = TransactionValue.CoinValue(wallet.token, value),
            fee = fee?.let { TransactionValue.CoinValue(wallet.token, it) },
            type = type,
            from = transaction.src,
            to = transaction.dest,
        )
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
    ): Flowable<List<TransactionRecord>> {
        val tonTransactionType = when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> TransactionType.Incoming
            FilterTransactionType.Outgoing -> TransactionType.Outgoing
            FilterTransactionType.Swap -> return Flowable.empty()
            FilterTransactionType.Approve -> return Flowable.empty()
        }

        return tonKit.newTransactionsFlow
            .map {
                if (tonTransactionType != null) {
                    it.filter { it.type == tonTransactionType.name }
                } else {
                    it
                }
            }
            .filter { it.isNotEmpty() }
            .map {
                it.map {
                    createTransactionRecord(it)
                }
            }
            .asFlowable()
    }

    override fun getTransactionUrl(transactionHash: String): String {
        return "https://tonscan.org/tx/$transactionHash"
    }

    private fun convertToAdapterState(syncState: SyncState) = when (syncState) {
        is SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SyncState.Synced -> AdapterState.Synced
        is SyncState.Syncing -> AdapterState.Syncing()
    }

    override val availableBalance: BigDecimal
        get() = balance

    override suspend fun send(amount: BigDecimal, address: String) {
        tonKit.send(address, amount.movePointRight(decimals).toBigInteger().toString())
    }
}

class TonTransactionRecord(
    uid: String,
    transactionHash: String,
    val logicalTime: Long,
    blockHeight: Int?,
    confirmationsThreshold: Int?,
    timestamp: Long,
    failed: Boolean = false,
    spam: Boolean = false,
    source: TransactionSource,
    override val mainValue: TransactionValue,
    val fee: TransactionValue?,
    val type: Type,
    val from: String?,
    val to: String?
) : TransactionRecord(
    uid,
    transactionHash,
    0,
    blockHeight,
    confirmationsThreshold,
    timestamp,
    failed,
    spam,
    source,
) {
    enum class Type {
        Incoming, Outgoing
    }
}