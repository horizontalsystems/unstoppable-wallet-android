package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.OxyraAdapter.Companion.DECIMALS
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.rx2.asFlowable

class OxyraTransactionsAdapter(
    private val kit: OxyraKit,
    private val transactionsProvider: OxyraTransactionsProvider,
    private val wallet: Wallet,
) : ITransactionsAdapter {

    override val explorerTitle: String = "Oxyra Explorer"

    override val transactionsState: AdapterState
        get() = kit.syncStateFlow.value.toAdapterState()

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = kit.syncStateFlow.asFlowable().map { }

    override val lastBlockInfo: LastBlockInfo?
        get() = kit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = kit.lastBlockUpdatedFlow.asFlowable()

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?
    ): Single<List<TransactionRecord>> {
        return transactionsProvider.getTransactions(from?.transactionHash, transactionType, address, limit)
            .map { transactions ->
                transactions.map {
                    getTransactionRecord(it)
                }
            }
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?
    ): Flowable<List<TransactionRecord>> {
        return transactionsProvider.getNewTransactionsFlowable(transactionType)
            .map { transactions ->
                transactions.map { getTransactionRecord(it) }
            }
    }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://oxyra-explorer.com/transaction/$transactionHash"

    private fun getTransactionRecord(transaction: OxyraTransactionInfo): TransactionRecord {
        val blockHeight = if (transaction.blockheight == 0L || transaction.isPending) null else transaction.blockheight.toInt()
        return when (transaction.direction) {
            OxyraTransactionInfo.Direction.Direction_In -> {
                val subaddress = kit.getSubaddress(transaction.accountIndex, transaction.addressIndex)
                BitcoinIncomingTransactionRecord(
                    token = wallet.token,
                    uid = transaction.hash,
                    transactionHash = transaction.hash,
                    transactionIndex = 0,
                    blockHeight = blockHeight,
                    confirmationsThreshold = OxyraTransactionInfo.CONFIRMATION,
                    timestamp = transaction.timestamp,
                    fee = transaction.fee.scaledDown(DECIMALS),
                    failed = transaction.isFailed,
                    lockInfo = null,
                    conflictingHash = null,
                    showRawTransaction = false,
                    amount = transaction.amount.scaledDown(DECIMALS),
                    from = null,
                    to = subaddress?.address,
                    memo = transaction.notes,
                    source = wallet.transactionSource
                )
            }

            OxyraTransactionInfo.Direction.Direction_Out -> {
                BitcoinOutgoingTransactionRecord(
                    token = wallet.token,
                    uid = transaction.hash,
                    transactionHash = transaction.hash,
                    transactionIndex = 0,
                    blockHeight = blockHeight,
                    confirmationsThreshold = OxyraTransactionInfo.CONFIRMATION,
                    timestamp = transaction.timestamp,
                    fee = transaction.fee.scaledDown(DECIMALS),
                    failed = transaction.isFailed,
                    lockInfo = null,
                    conflictingHash = null,
                    showRawTransaction = false,
                    amount = transaction.amount.scaledDown(DECIMALS).negate(),
                    to = null,
                    sentToSelf = false,
                    memo = transaction.notes,
                    source = wallet.transactionSource,
                    replaceable = false
                )
            }
        }
    }
}

// Oxyra Transaction Info data class
data class OxyraTransactionInfo(
    val hash: String,
    val amount: Long,
    val fee: Long,
    val timestamp: Long,
    val blockheight: Long,
    val confirmations: Int,
    val isPending: Boolean,
    val isFailed: Boolean,
    val direction: Direction,
    val accountIndex: Int,
    val addressIndex: Int,
    val notes: String?
) {
    enum class Direction {
        Direction_In,
        Direction_Out
    }

    companion object {
        const val CONFIRMATION = 10
    }
}

