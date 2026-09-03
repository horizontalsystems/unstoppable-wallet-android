package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.managers.ISpamOutgoingContextSource
import io.horizontalsystems.walletkit.core.managers.PoisoningScorer
import io.horizontalsystems.walletkit.core.managers.SolanaKitWrapper
import io.horizontalsystems.walletkit.core.managers.SolanaTransactionEventExtractor
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.solanakit.SolanaKit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class SolanaTransactionsAdapter(
        solanaKitWrapper: SolanaKitWrapper,
        private val solanaTransactionConverter: SolanaTransactionConverter
) : ITransactionsAdapter, ISpamOutgoingContextSource {

    private val kit = solanaKitWrapper.solanaKit
    private val spamContextExtractor = SolanaTransactionEventExtractor()

    // Supplies the recent counterparties that SpamManager correlates incoming dust senders against
    // for address-poisoning detection. Without this, Solana gray-zone transactions (dust, zero-value
    // NFTs) could never accrue the prefix/suffix/time points needed to cross the spam threshold.
    //
    // We pull BOTH directions (incoming = null): Solana poisoning most often mimics the sender of an
    // INCOMING payment, not an address the user sent to — and a watch/receive-only wallet has no
    // outgoing history at all, so an outgoing-only context would always be empty. `transactionHash`
    // is the UTF-8 encoding of the incoming tx's base58 signature (see SolanaTransactionConverter),
    // and `getAllTransactions(fromHash = ...)` returns strictly older transactions — the correct
    // context window. Solana carries no confirmed slot, so block correlation is unavailable; the
    // extractor leaves blockHeight null.
    override suspend fun getOutgoingContext(
        transactionHash: ByteArray,
        operationId: Long?,
        limit: Int
    ): List<PoisoningScorer.OutgoingTxInfo> {
        val userAddress = kit.receiveAddress
        val fromHash = String(transactionHash, Charsets.UTF_8)
        return kit.getAllTransactions(incoming = null, fromHash = fromHash, limit = limit)
            .sortedByDescending { it.transaction.timestamp }
            .mapNotNull { spamContextExtractor.extractCounterpartyInfo(it, userAddress) }
    }

    override val explorerTitle: String
        get() = "Solscan.io"

    override fun getTransactionUrl(transactionHash: String): String =
        "https://solscan.io/tx/$transactionHash"

    override val lastBlockInfo: LastBlockInfo?
        get() = kit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlow: Flow<Unit>
        get() = kit.lastBlockHeightFlow.map {}

    override val transactionsState: AdapterState
        get() = convertToAdapterState(kit.transactionsSyncState)

    override val transactionsStateUpdatedFlow: Flow<Unit>
        get() = kit.transactionsSyncStateFlow.map {}

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> = when (address) {
        null -> getTransactionsList(from, token, limit, transactionType)
        else -> listOf()
    }

    private suspend fun getTransactionsList(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
    ): List<TransactionRecord> {
        val incoming = when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> true
            FilterTransactionType.Outgoing -> false
            else -> return listOf()
        }

        val transactions = when {
            token == null -> kit.getAllTransactions(incoming, from?.transactionHash, limit)
            token.type is TokenType.Native -> kit.getSolTransactions(incoming, from?.transactionHash, limit)
            token.type is TokenType.Spl -> kit.getSplTransactions((token.type as TokenType.Spl).address, incoming, from?.transactionHash, limit)
            else -> listOf()
        }

        return transactions.map { solanaTransactionConverter.transactionRecord(it) }
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> = when (address) {
        null -> getTransactionRecordsFlow(token, transactionType)
        else -> emptyFlow()
    }

    private fun getTransactionRecordsFlow(token: Token?, transactionType: FilterTransactionType): Flow<List<TransactionRecord>> {
        val incoming: Boolean? = when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> true
            FilterTransactionType.Outgoing -> false
            else -> return emptyFlow()
        }

        val transactionsFlow = when {
            token == null -> kit.allTransactionsFlow(incoming)
            token.type is TokenType.Native -> kit.solTransactionsFlow(incoming)
            token.type is TokenType.Spl -> kit.splTransactionsFlow((token.type as TokenType.Spl).address, incoming)
            else -> emptyFlow()
        }

        return transactionsFlow.map { txList ->
            txList.map { solanaTransactionConverter.transactionRecord(it) }
        }
    }

    private fun convertToAdapterState(syncState: SolanaKit.SyncState): AdapterState =
            when (syncState) {
                is SolanaKit.SyncState.Synced -> AdapterState.Synced
                is SolanaKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
                is SolanaKit.SyncState.Syncing -> AdapterState.Syncing()
            }

    companion object {
        const val decimal = 18

        fun clear(walletId: String) {
            SolanaKit.clear(App.instance, walletId)
        }
    }
}
