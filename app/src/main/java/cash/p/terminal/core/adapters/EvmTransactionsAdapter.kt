package cash.p.terminal.core.adapters

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.core.App
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.Token
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.TransactionTag
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.reactivex.Flowable
import io.reactivex.Single

class EvmTransactionsAdapter(
    val evmKitWrapper: EvmKitWrapper,
    baseToken: Token,
    coinManager: ICoinManager,
    source: TransactionSource,
    private val evmTransactionSource: io.horizontalsystems.ethereumkit.models.TransactionSource,
    evmLabelManager: EvmLabelManager
) : ITransactionsAdapter {

    private val evmKit = evmKitWrapper.evmKit
    private val transactionConverter = EvmTransactionConverter(coinManager, evmKitWrapper, source, App.spamManager, baseToken, evmLabelManager)

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

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ): Single<List<TransactionRecord>> {
        return evmKit.getFullTransactionsAsync(
            getFilters(token, transactionType, address?.lowercase()),
            from?.transactionHash?.hexStringToByteArray(),
            limit
        ).map {
            it.map { tx -> transactionConverter.transactionRecord(tx) }
        }
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flowable<List<TransactionRecord>> {
        return evmKit.getFullTransactionsFlowable(getFilters(token, transactionType, address)).map {
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
