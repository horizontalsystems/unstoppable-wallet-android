package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.MoneroAdapter.Companion.DECIMALS
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.monero.MoneroIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.monero.MoneroOutgoingTransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.monerokit.MoneroKit
import io.horizontalsystems.monerokit.model.TransactionInfo
import io.horizontalsystems.monerokit.model.TransactionInfo.Direction
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.rx2.asFlowable

class MoneroTransactionsAdapter(
    private val kit: MoneroKit,
    private val transactionsProvider: MoneroTransactionsProvider,
    private val wallet: Wallet,
) : ITransactionsAdapter {

    override val explorerTitle: String = "Monero Explorer"

    override val transactionsState: AdapterState
        get() = kit.syncStateFlow.value.toAdapterState()

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = kit.syncStateFlow.asFlowable().map { }

    override val lastBlockInfo: LastBlockInfo?
        get() = kit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = kit.lastBlockUpdatedFlow.asFlowable()

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?
    ): List<TransactionRecord> {
        return transactionsProvider.getTransactions(from?.transactionHash, transactionType, address, limit)
            .map { getTransactionRecord(it) }
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?
    ): Flow<List<TransactionRecord>> {
        val newTransactions = transactionsProvider.getNewTransactionsFlow(transactionType)
            .map { transactions ->
                transactions.map { getTransactionRecord(it) }
            }
        // any emission makes TransactionAdapterWrapper drop its cache and re-query,
        // which is exactly what an account switch needs
        val accountSwitch = transactionsProvider.activeAccountFlow.drop(1)
            .map { emptyList<TransactionRecord>() }

        return merge(newTransactions, accountSwitch)
    }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://blockchair.com/monero/transaction/$transactionHash"

    private fun getTransactionRecord(transaction: TransactionInfo): TransactionRecord {
        val blockHeight = if (transaction.blockheight == 0L || transaction.isPending) null else transaction.blockheight.toInt()
        return when (transaction.direction) {
            Direction.Direction_In -> {
                val subaddress = kit.getSubaddress(transaction.accountIndex, transaction.addressIndex)
                MoneroIncomingTransactionRecord(
                    token = wallet.token,
                    uid = transaction.hash,
                    transactionHash = transaction.hash,
                    transactionIndex = 0,
                    blockHeight = blockHeight,
                    confirmationsThreshold = TransactionInfo.CONFIRMATION,
                    timestamp = transaction.timestamp,
                    fee = transaction.fee.scaledDown(DECIMALS),
                    failed = transaction.isFailed,
                    amount = transaction.amount.scaledDown(DECIMALS),
                    from = null,
                    to = subaddress?.address,
                    memo = transaction.notes,
                    source = wallet.transactionSource
                )
            }

            Direction.Direction_Out -> {
                MoneroOutgoingTransactionRecord(
                    token = wallet.token,
                    uid = transaction.hash,
                    transactionHash = transaction.hash,
                    transactionIndex = 0,
                    blockHeight = blockHeight,
                    confirmationsThreshold = TransactionInfo.CONFIRMATION,
                    timestamp = transaction.timestamp,
                    fee = transaction.fee.scaledDown(DECIMALS),
                    failed = transaction.isFailed,
                    amount = transaction.amount.scaledDown(DECIMALS).negate(),
                    to = if (transaction.transfers.isNullOrEmpty()) null else transaction.transfers[0].address,
                    sentToSelf = false,
                    memo = transaction.notes,
                    source = wallet.transactionSource,
                    txKey = kit.getTxKey(transaction.hash)
                )
            }
        }
    }
}
