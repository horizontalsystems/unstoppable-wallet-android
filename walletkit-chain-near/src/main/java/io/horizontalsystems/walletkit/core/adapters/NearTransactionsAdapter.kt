package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.models.Transaction
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.factories.NearTransactionConverter
import io.horizontalsystems.walletkit.core.managers.NearKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class NearTransactionsAdapter(
    kitWrapper: NearKitWrapper,
    private val converter: NearTransactionConverter,
) : ITransactionsAdapter {
    private val kit = kitWrapper.kit
    private val selfAccount = kit.accountId

    override val explorerTitle = "NearBlocks"
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
        val tag = tag(token) ?: return emptyList()
        val matches = filter(transactionType, address) ?: return emptyList()

        // The kit pages its local table by (timestamp, hash); filters are applied here, so keep
        // reading pages until enough records match or the history ends.
        val result = mutableListOf<Transaction>()
        var beforeTimestamp = from?.timestamp
        var beforeHash = from?.transactionHash
        while (result.size < limit) {
            val page = kit.getTransactions(tag.value, beforeTimestamp, beforeHash, PAGE_SIZE)
            result += page.filter(matches)
            if (page.size < PAGE_SIZE) break
            beforeTimestamp = page.last().timestamp
            beforeHash = page.last().hash
        }
        return result.take(limit).map { converter.convert(it) }
    }

    override suspend fun getTransactionsAfter(fromTransactionId: String?): List<TransactionRecord> {
        val anchor = fromTransactionId?.let { kit.getTransaction(it) }
        val result = mutableListOf<Transaction>()
        var beforeTimestamp: Long? = null
        var beforeHash: String? = null
        while (true) {
            val page = kit.getTransactions(null, beforeTimestamp, beforeHash, PAGE_SIZE)
            val newer = if (anchor == null) page else page.takeWhile { it.timestamp > anchor.timestamp }
            result += newer
            if (page.size < PAGE_SIZE || newer.size < page.size) break
            beforeTimestamp = page.last().timestamp
            beforeHash = page.last().hash
        }
        return result.map { converter.convert(it) }
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        val tag = tag(token) ?: return emptyFlow()
        val matches = filter(transactionType, address) ?: return emptyFlow()
        return kit.transactionsFlow.map { changed ->
            changed
                .filter { tx -> tag.value == null || tx.belongsTo(tag.value) }
                .filter(matches)
                .map { converter.convert(it) }
        }
    }

    override fun getTransactionUrl(transactionHash: String): String = kit.network.transactionUrl(transactionHash)

    private class Tag(val value: String?)

    /** Null when the token is not a NEAR token at all. */
    private fun tag(token: Token?): Tag? = when (val type = token?.type) {
        null -> Tag(null)
        TokenType.Native -> Tag(NearKit.TOKEN_NATIVE)
        is TokenType.Nep141 -> Tag(type.contractId)
        else -> null
    }

    /** Mirrors the kit's history tags for records that arrive through [NearKit.transactionsFlow]. */
    private fun Transaction.belongsTo(tag: String): Boolean =
        if (tag == NearKit.TOKEN_NATIVE) {
            isSigner(selfAccount) || nearTransfers.any { (it.from == selfAccount || it.to == selfAccount) && it.amount.signum() > 0 }
        } else {
            ftTransfers.any { it.contractId == tag } || (isSigner(selfAccount) && receiverId == tag)
        }

    /** Null when the filter combination has no matches on NEAR (no swaps or approvals yet). */
    private fun filter(transactionType: FilterTransactionType, address: String?): ((Transaction) -> Boolean)? {
        val typeMatches: (Transaction) -> Boolean = when (transactionType) {
            FilterTransactionType.All -> { _ -> true }
            FilterTransactionType.Incoming -> { tx -> !tx.isSigner(selfAccount) }
            FilterTransactionType.Outgoing -> { tx -> tx.isSigner(selfAccount) }
            FilterTransactionType.Swap,
            FilterTransactionType.Approve -> return null
        }

        val addressMatches: (Transaction) -> Boolean = if (address == null) {
            { _ -> true }
        } else {
            { tx ->
                tx.signerId == address || tx.receiverId == address ||
                    tx.ftTransfers.any { it.from == address || it.to == address }
            }
        }

        return { tx -> typeMatches(tx) && addressMatches(tx) }
    }

    companion object {
        private const val PAGE_SIZE = 100
    }
}
