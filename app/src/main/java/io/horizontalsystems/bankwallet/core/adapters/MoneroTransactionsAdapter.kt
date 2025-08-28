package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.adapters.MoneroAdapter.Companion.DECIMALS
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.monerokit.MoneroKit
import io.horizontalsystems.monerokit.model.TransactionInfo
import io.horizontalsystems.monerokit.model.TransactionInfo.Direction
import io.reactivex.Flowable
import io.reactivex.Single
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
        "https://blockchair.com/monero/transaction/$transactionHash"

    private fun getTransactionRecord(transaction: TransactionInfo): TransactionRecord {
        val blockHeight = if (transaction.blockheight == 0L || transaction.isPending) null else transaction.blockheight.toInt()
        return when (transaction.direction) {
            Direction.Direction_In -> {
                val subaddress = kit.getSubaddress(transaction.accountIndex, transaction.addressIndex)
                BitcoinIncomingTransactionRecord(
                    token = wallet.token,
                    uid = transaction.hash,
                    transactionHash = transaction.hash,
                    transactionIndex = 0,
                    blockHeight = blockHeight,
                    confirmationsThreshold = TransactionInfo.CONFIRMATION,
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

            Direction.Direction_Out -> {
                BitcoinOutgoingTransactionRecord(
                    token = wallet.token,
                    uid = transaction.hash,
                    transactionHash = transaction.hash,
                    transactionIndex = 0,
                    blockHeight = blockHeight,
                    confirmationsThreshold = TransactionInfo.CONFIRMATION,
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
