package cash.p.terminal.core.adapters

import cash.p.terminal.core.App
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.data.repository.EvmTransactionRepository
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.TransactionTag
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class EvmTransactionsAdapter(
    private val evmTransactionRepository: EvmTransactionRepository,
    baseToken: Token,
    coinManager: ICoinManager,
    source: TransactionSource,
    private val evmTransactionSource: io.horizontalsystems.ethereumkit.models.TransactionSource,
    evmLabelManager: EvmLabelManager
) : ITransactionsAdapter {

    private val transactionConverter = EvmTransactionConverter(
        coinManager,
        evmTransactionRepository,
        source,
        App.spamManager,
        baseToken,
        evmLabelManager
    )

    override val explorerTitle: String
        get() = evmTransactionSource.name

    override fun getTransactionUrl(transactionHash: String): String =
        evmTransactionSource.transactionUrl(transactionHash)

    override val lastBlockInfo: LastBlockInfo?
        get() = evmTransactionRepository.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = evmTransactionRepository.lastBlockHeightFlowable.map { }

    override val transactionsState: AdapterState
        get() = convertToAdapterState(evmTransactionRepository.transactionsSyncState)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = evmTransactionRepository.transactionsSyncStateFlowable.map {}

    override val additionalTokenQueries: List<TokenQuery>
        get() = evmTransactionRepository.getTagTokenContractAddresses().map { address ->
            TokenQuery(evmTransactionRepository.getBlockchainType(), TokenType.Eip20(address))
        }

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> {
        return evmTransactionRepository.getFullTransactionsAsync(
            getFilters(token, transactionType, address?.lowercase()),
            from?.transactionHash?.hexStringToByteArray(),
            limit
        ).map { tx ->
            transactionConverter.transactionRecord(tx)
        }
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        return evmTransactionRepository.getFullTransactionsFlowable(
            getFilters(
                token,
                transactionType,
                address
            )
        ).map {
            it.map { tx -> transactionConverter.transactionRecord(tx) }
        }
    }

    private fun convertToAdapterState(syncState: EthereumKit.SyncState): AdapterState =
        when (syncState) {
            is EthereumKit.SyncState.Synced -> AdapterState.Synced
            is EthereumKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is EthereumKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    private fun coinTagName(token: Token) = when (val type = token.type) {
        TokenType.Native -> TransactionTag.EVM_COIN
        is TokenType.Eip20 -> type.address
        else -> ""
    }

    private fun getFilters(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ) = buildList {
        token?.let {
            add(listOf(coinTagName(it)))
        }

        val filterType = when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> when {
                token != null -> TransactionTag.tokenIncoming(coinTagName(token))
                else -> TransactionTag.INCOMING
            }

            FilterTransactionType.Outgoing -> when {
                token != null -> TransactionTag.tokenOutgoing(coinTagName(token))
                else -> TransactionTag.OUTGOING
            }

            FilterTransactionType.Swap -> TransactionTag.SWAP
            FilterTransactionType.Approve -> TransactionTag.EIP20_APPROVE
        }

        filterType?.let {
            add(listOf(it))
        }

        if (!address.isNullOrBlank()) {
            add(listOf("from_$address", "to_$address"))
        }
    }
}
