package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.xrpkit.models.Amount
import io.horizontalsystems.xrpkit.models.Transaction
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.factories.XrpTransactionConverter
import io.horizontalsystems.walletkit.core.managers.XrpKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class XrpTransactionsAdapter(
    xrpKitWrapper: XrpKitWrapper,
    private val converter: XrpTransactionConverter,
) : ITransactionsAdapter {
    private val kit = xrpKitWrapper.kit
    private val selfAddress = kit.receiveAddress

    override val explorerTitle = "XRPL Explorer"
    override val transactionsState: AdapterState
        get() = kit.transactionsSyncState.toAdapterState()
    override val transactionsStateUpdatedFlow: Flow<Unit>
        get() = kit.transactionsSyncStateFlow.map {}
    override val lastBlockInfo: LastBlockInfo?
        get() = null
    override val lastBlockUpdatedFlow: Flow<Unit>
        get() = emptyFlow()

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> {
        val filter = filter(token, transactionType, address) ?: return emptyList()

        // The kit keeps the full (capped) history locally, newest first; paging happens here.
        val all = kit.getAllTransactions().filter(filter)
        val start = from?.let { record -> all.indexOfFirst { it.hash == record.transactionHash } + 1 } ?: 0
        return all.drop(start).take(limit).map { converter.convert(it) }
    }

    override suspend fun getTransactionsAfter(fromTransactionId: String?): List<TransactionRecord> {
        val all = kit.getAllTransactions()
        val fromTimestamp = fromTransactionId?.let { id -> all.firstOrNull { it.hash == id }?.timestamp }
        return all
            .filter { fromTimestamp == null || it.timestamp > fromTimestamp }
            .map { converter.convert(it) }
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        val filter = filter(token, transactionType, address) ?: return emptyFlow()
        return kit.transactionsFlow.map { changed ->
            changed.filter(filter).map { converter.convert(it) }
        }
    }

    override fun getTransactionUrl(transactionHash: String): String = kit.network.transactionUrl(transactionHash)

    /** Null when the filter combination has no matches on XRPL (no swaps or approvals). */
    private fun filter(token: Token?, transactionType: FilterTransactionType, address: String?): ((Transaction) -> Boolean)? {
        val typeMatches: (Transaction) -> Boolean = when (transactionType) {
            FilterTransactionType.All -> { _ -> true }
            FilterTransactionType.Incoming -> { tx -> tx.isIncoming(selfAddress) }
            FilterTransactionType.Outgoing -> { tx -> tx.isOutgoing(selfAddress) }
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> return null
        }

        val tokenMatches: (Transaction) -> Boolean = when (val tokenType = token?.type) {
            null -> { _ -> true }
            TokenType.Native -> { tx -> tx.type != "Payment" || tx.amount is Amount.Xrp }
            is TokenType.XrpAsset -> { tx ->
                val amount = tx.amount as? Amount.Issued ?: tx.limitAmount as? Amount.Issued
                amount != null && amount.currency == tokenType.currency && amount.issuer == tokenType.issuer
            }
            else -> return null
        }

        val addressMatches: (Transaction) -> Boolean = if (address == null) {
            { _ -> true }
        } else {
            { tx -> tx.account == address || tx.destination == address }
        }

        return { tx -> typeMatches(tx) && tokenMatches(tx) && addressMatches(tx) }
    }
}
