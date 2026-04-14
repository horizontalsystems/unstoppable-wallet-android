package com.quantum.wallet.bankwallet.core.adapters

import com.quantum.wallet.bankwallet.core.AdapterState
import com.quantum.wallet.bankwallet.core.ITransactionsAdapter
import com.quantum.wallet.bankwallet.core.adapters.MoneroAdapter.Companion.DECIMALS
import com.quantum.wallet.bankwallet.entities.LastBlockInfo
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.entities.transactionrecords.TransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.monero.MoneroIncomingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.monero.MoneroOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.monerokit.MoneroKit
import io.horizontalsystems.monerokit.model.TransactionInfo
import io.horizontalsystems.monerokit.model.TransactionInfo.Direction
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        return transactionsProvider.getNewTransactionsFlow(transactionType)
            .map { transactions ->
                transactions.map { getTransactionRecord(it) }
            }
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
