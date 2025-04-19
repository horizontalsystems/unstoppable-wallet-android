package cash.p.terminal.core.adapters

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.core.App
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.SolanaKitWrapper
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.solanakit.SolanaKit
import io.reactivex.Flowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.rxSingle

class SolanaTransactionsAdapter(
        solanaKitWrapper: SolanaKitWrapper,
        private val solanaTransactionConverter: SolanaTransactionConverter
) : ITransactionsAdapter {

    private val kit = solanaKitWrapper.solanaKit

    override val explorerTitle: String
        get() = "Solscan.io"

    override fun getTransactionUrl(transactionHash: String): String =
        "https://solscan.io/tx/$transactionHash"

    override val lastBlockInfo: LastBlockInfo?
        get() = kit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = kit.lastBlockHeightFlow.map {}.asFlowable()

    override val transactionsState: AdapterState
        get() = convertToAdapterState(kit.transactionsSyncState)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = kit.transactionsSyncStateFlow.map {}.asFlowable()

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ) = when (address) {
        null -> getTransactionsAsync(from, token, limit, transactionType)
        else -> emptyList<TransactionRecord>()
    }

    private suspend fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
    ): List<TransactionRecord> {
        val incoming = when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> true
            FilterTransactionType.Outgoing -> false
            else -> return emptyList()
        }

        val transactions = rxSingle(Dispatchers.IO) {
            when {
                token == null -> kit.getAllTransactions(incoming, from?.transactionHash, limit)
                token.type is TokenType.Native -> kit.getSolTransactions(incoming, from?.transactionHash, limit)
                token.type is TokenType.Spl -> kit.getSplTransactions((token.type as TokenType.Spl).address, incoming, from?.transactionHash, limit)
                else -> listOf()
            }
        }

        return transactions.map { txList ->
            txList.map { solanaTransactionConverter.transactionRecord(it) }
        }.await()
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> = when (address) {
        null -> getTransactionRecordsFlowable(token, transactionType).asFlow()
        else -> emptyFlow()
    }

    private fun getTransactionRecordsFlowable(token: Token?, transactionType: FilterTransactionType): Flowable<List<TransactionRecord>> {
        val incoming: Boolean? = when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> true
            FilterTransactionType.Outgoing -> false
            else -> return Flowable.just(listOf())
        }

        val transactionsFlow =  when {
            token == null -> kit.allTransactionsFlow(incoming)
            token.type is TokenType.Native -> kit.solTransactionsFlow(incoming)
            token.type is TokenType.Spl -> kit.splTransactionsFlow((token.type as TokenType.Spl).address, incoming)
            else -> emptyFlow()
        }

        return transactionsFlow.map { txList ->
            txList.map { solanaTransactionConverter.transactionRecord(it) }
        }.asFlowable()
    }

    private fun convertToAdapterState(syncState: SolanaKit.SyncState): AdapterState =
            when (syncState) {
                is SolanaKit.SyncState.Synced -> AdapterState.Synced
                is SolanaKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
                is SolanaKit.SyncState.Syncing -> AdapterState.Syncing()
            }

    companion object {
        const val decimal = 9

        fun clear(walletId: String) {
            SolanaKit.clear(App.instance, walletId)
        }
    }
}
