package cash.p.terminal.core.adapters

import cash.p.terminal.core.AdapterState
import cash.p.terminal.core.BalanceData
import cash.p.terminal.core.IAdapter
import cash.p.terminal.core.IBalanceAdapter
import cash.p.terminal.core.IReceiveAdapter
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.Wallet
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tonkit.TonKit
import io.horizontalsystems.tonkit.TonKitFactory
import io.horizontalsystems.tonkit.entities.TonTransaction
import io.horizontalsystems.tonkit.entities.TransactionType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.rx2.rxSingle

class TonAdapter(
    private val wallet: Wallet,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter {

    private val adapterStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val tonKit: TonKit

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        val accountType = wallet.account.type
        if (accountType is AccountType.Mnemonic) {
            tonKit = TonKitFactory.create(accountType.words, accountType.passphrase)
        } else {
            throw UnsupportedAccountException()
        }
    }

    override fun start() {
        tonKit.start()
    }

    override fun stop() {
        tonKit.stop()
        coroutineScope.cancel()
    }

    override fun refresh() {
        tonKit.refresh()
    }

    override val debugInfo: String
        get() = ""


    override val balanceState: AdapterState = AdapterState.Synced
    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = adapterStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val balanceData: BalanceData
        get() = BalanceData(tonKit.balance)
    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = Flowable.empty()

    override val receiveAddress = tonKit.address
    override val isMainNet = true

    override val explorerTitle = "tonscan.org"
    override val transactionsState: AdapterState
        get() = AdapterState.NotSynced(Exception("Not Started"))
    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = Flowable.empty()
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
        tonKit
            .transactions(
                limit,
                from?.transactionHash,
                (from as? TonTransactionRecord)?.logicalTime
            )
            .map {
                createTransactionRecord(it)
            }
    }

    private fun createTransactionRecord(transaction: TonTransaction): TransactionRecord {
        val value = when (transaction.type) {
            TransactionType.Incoming -> transaction.value
            TransactionType.Outgoing -> transaction.value.negate()
        }

        return TonTransactionRecord(
            uid = transaction.hash,
            transactionHash = transaction.hash,
            logicalTime = transaction.lt,
            blockHeight = null,
            confirmationsThreshold = null,
            timestamp = transaction.timestamp,
            source = wallet.transactionSource,
            mainValue = TransactionValue.CoinValue(wallet.token, value)
        )
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
    ) = Flowable.empty<List<TransactionRecord>>()

    override fun getTransactionUrl(transactionHash: String): String {
        return "https://tonscan.org/tx/$transactionHash"
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
    override val mainValue: TransactionValue?
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
)