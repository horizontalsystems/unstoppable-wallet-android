package cash.p.terminal.core.adapters

import cash.p.terminal.core.AdapterState
import cash.p.terminal.core.App
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionTag
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
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
    private val transactionConverter = EvmTransactionConverter(coinManager, evmKitWrapper, source, baseToken, evmLabelManager)

    override val explorerTitle: String
        get() = evmTransactionSource.name

    override fun getTransactionUrl(transactionHash: String): String? =
        evmTransactionSource.transactionUrl(transactionHash)

    override val lastBlockInfo: LastBlockInfo?
        get() = evmKit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = evmKit.lastBlockHeightFlowable.map { }

    override val transactionsState: AdapterState
        get() = convertToAdapterState(evmKit.transactionsSyncState)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = evmKit.transactionsSyncStateFlowable.map {}

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>> {
        return evmKit.getFullTransactionsAsync(
            getFilters(token, transactionType),
            from?.transactionHash?.hexStringToByteArray(),
            limit
        ).map {
            it.map { tx -> transactionConverter.transactionRecord(tx) }
        }
    }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType
    ): Flowable<List<TransactionRecord>> {
        return evmKit.getFullTransactionsFlowable(getFilters(token, transactionType)).map {
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

    private fun getFilters(token: Token?, filter: FilterTransactionType): List<List<String>> {
        val filterCoin = token?.let {
            coinTagName(it)
        }

        val filterTag = when (filter) {
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

        return listOfNotNull(filterCoin, filterTag).map { listOf(it) }
    }

    companion object {
        const val decimal = 18

        fun clear(walletId: String, testMode: Boolean) {
            val networkTypes = when {
                testMode -> listOf(Chain.EthereumGoerli)
                else -> listOf(
                    Chain.Ethereum,
                    Chain.BinanceSmartChain,
                    Chain.Polygon,
                    Chain.Optimism,
                    Chain.ArbitrumOne,
                    Chain.Gnosis,
                )
            }
            networkTypes.forEach {
                EthereumKit.clear(App.instance, it, walletId)
            }
        }
    }
}
