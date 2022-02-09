package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.TransactionTag
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Flowable
import io.reactivex.Single

class EvmTransactionsAdapter(
    val evmKitWrapper: EvmKitWrapper,
    baseCoin: PlatformCoin,
    coinManager: ICoinManager,
    source: TransactionSource
) : ITransactionsAdapter {

    private val evmKit = evmKitWrapper.evmKit
    private val transactionConverter = EvmTransactionConverter(coinManager, evmKit, source, baseCoin)

    override val lastBlockInfo: LastBlockInfo?
        get() = evmKit.lastBlockHeight?.toInt()?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = evmKit.lastBlockHeightFlowable.map { }

    override val transactionsState: AdapterState
        get() = convertToAdapterState(evmKit.transactionsSyncState)

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = evmKit.transactionsSyncStateFlowable.map {}

    override val explorerTitle: String
        get() = when (evmKit.networkType) {
            EthereumKit.NetworkType.EthMainNet,
            EthereumKit.NetworkType.EthRopsten,
            EthereumKit.NetworkType.EthKovan,
            EthereumKit.NetworkType.EthRinkeby,
            EthereumKit.NetworkType.EthGoerli -> "etherscan.io"
            EthereumKit.NetworkType.BscMainNet -> "bscscan.com"
        }

    override fun explorerUrl(transactionHash: String): String {
        val domain = when (evmKit.networkType) {
            EthereumKit.NetworkType.EthMainNet -> "etherscan.io"
            EthereumKit.NetworkType.EthRopsten -> "ropsten.etherscan.io"
            EthereumKit.NetworkType.EthKovan -> "kovan.etherscan.io"
            EthereumKit.NetworkType.EthRinkeby -> "rinkeby.etherscan.io"
            EthereumKit.NetworkType.BscMainNet -> "bscscan.com"
            EthereumKit.NetworkType.EthGoerli -> "goerli.etherscan.io"
        }

        return "https://$domain/tx/$transactionHash"
    }

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        coin: PlatformCoin?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>> {
        return evmKit.getTransactionsAsync(
            getFilters(coin, transactionType),
            from?.transactionHash?.hexStringToByteArray(),
            limit
        ).map {
            it.map { tx -> transactionConverter.transactionRecord(tx) }
        }
    }

    override fun getTransactionRecordsFlowable(
        coin: PlatformCoin?,
        transactionType: FilterTransactionType
    ): Flowable<List<TransactionRecord>> {
        return evmKit.getTransactionsFlowable(getFilters(coin, transactionType)).map {
            it.map { tx -> transactionConverter.transactionRecord(tx) }
        }
    }

    private fun convertToAdapterState(syncState: EthereumKit.SyncState): AdapterState =
        when (syncState) {
            is EthereumKit.SyncState.Synced -> AdapterState.Synced
            is EthereumKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is EthereumKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    private fun coinTagName(coin: PlatformCoin) = when (val type = coin.coinType) {
        CoinType.Ethereum, CoinType.BinanceSmartChain -> TransactionTag.EVM_COIN
        is CoinType.Erc20 -> type.address
        is CoinType.Bep20 -> type.address
        else -> throw IllegalArgumentException()
    }

    private fun getFilters(coin: PlatformCoin?, filter: FilterTransactionType): List<List<String>> {
        val filterCoin = coin?.let {
            coinTagName(it)
        }

        val filterTag = when (filter) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> when {
                coin != null -> TransactionTag.eip20Incoming(coinTagName(coin))
                else -> TransactionTag.INCOMING
            }
            FilterTransactionType.Outgoing -> when {
                coin != null -> TransactionTag.eip20Outgoing(coinTagName(coin))
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
                testMode -> listOf(EthereumKit.NetworkType.EthRopsten)
                else -> listOf(
                    EthereumKit.NetworkType.EthMainNet,
                    EthereumKit.NetworkType.BscMainNet
                )
            }
            networkTypes.forEach {
                EthereumKit.clear(App.instance, it, walletId)
            }
        }
    }
}
