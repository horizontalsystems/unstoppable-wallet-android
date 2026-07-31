package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.managers.EvmKitWrapper
import io.horizontalsystems.walletkit.core.managers.EvmLabelManager
import io.horizontalsystems.walletkit.core.managers.EvmTransactionConverterResolver
import io.horizontalsystems.walletkit.entities.LastBlockInfo
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.FilterTransactionType
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.hexStringToByteArrayOrNull
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionTag
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await

class EvmTransactionsAdapter(
    val evmKitWrapper: EvmKitWrapper,
    val baseToken: Token,
    coinManager: ICoinManager,
    source: TransactionSource,
    private val evmTransactionSource: io.horizontalsystems.ethereumkit.models.TransactionSource,
    evmLabelManager: EvmLabelManager
) : ITransactionsAdapter {

    private val evmKit = evmKitWrapper.evmKit
    private val transactionConverter = EvmTransactionConverter(coinManager, evmKitWrapper, source, baseToken, evmLabelManager)

    // A registered converter fully replaces the stock conversion for this
    // adapter; rows it maps to null stay hidden.
    private val customConverter = EvmTransactionConverterResolver.provider
        ?.converter(source, baseToken, evmKit.receiveAddress)

    private suspend fun records(fullTransactions: List<FullTransaction>, token: Token?): List<TransactionRecord> =
        customConverter?.let { converter -> fullTransactions.mapNotNull { converter.convert(it, token) } }
            ?: fullTransactions.map { transactionConverter.transactionRecord(it) }

    override val explorerTitle: String
        get() = evmTransactionSource.name

    override fun getTransactionUrl(transactionHash: String): String =
        evmTransactionSource.transactionUrl(transactionHash)

    override val lastBlockInfo: LastBlockInfo?
        get() = evmKit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = evmKit.lastBlockHeightFlowable.map { }

    override val transactionsState: AdapterState
        get() = convertToAdapterState(evmKit.transactionsSyncState)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = evmKit.transactionsSyncStateFlowable.map {}

    override val additionalTokenQueries: List<TokenQuery>
        get() = evmKit.getTagTokenContractAddresses().map { address ->
            TokenQuery(evmKitWrapper.blockchainType, TokenType.Eip20(address))
        }

    override suspend fun getTransactions(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): List<TransactionRecord> {
        return records(
            evmKit.getFullTransactionsAsync(
                getFilters(token, transactionType, address?.lowercase()),
                from?.transactionHash?.hexStringToByteArray(),
                limit
            ).await(),
            token
        )
    }

    override suspend fun getTransactionsAfter(fromTransactionId: String?): List<TransactionRecord> {
        return records(
            evmKit.getFullTransactionsAfterSingle(fromTransactionId?.hexStringToByteArrayOrNull()).await(),
            null
        )
    }

    override suspend fun getFullTransactionsBefore(
        fromTransactionHash: ByteArray?,
        limit: Int
    ): List<FullTransaction> {
        return evmKit.getFullTransactionsAsync(
            emptyList(),
            fromTransactionHash,
            limit
        ).await()
    }

    override fun getTransactionRecordsFlow(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flow<List<TransactionRecord>> {
        return evmKit.getFullTransactionsFlowable(getFilters(token, transactionType, address))
            .asFlow()
            .map { records(it, token) }
    }

    private fun convertToAdapterState(syncState: EthereumKit.SyncState): AdapterState =
        when (syncState) {
            is EthereumKit.SyncState.Synced -> AdapterState.Synced
            is EthereumKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is EthereumKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    private fun coinTagName(token: Token) = when (val type = token.type) {
        TokenType.Native -> TransactionTag.EVM_COIN
        // EvmKit writes transaction tags with the contract address lowercased (Address.hex),
        // but a user-added custom token can carry a checksummed address — normalize, or the
        // token's history filter never matches (catalog tokens are already lowercase).
        is TokenType.Eip20 -> type.address.lowercase()
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
