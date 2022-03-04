package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.EvmNetwork
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource

class EvmNetworkManager(private val appConfigProvider: AppConfigProvider) {

    val ethereumNetworks: List<EvmNetwork>
        get() = listOfNotNull(
            defaultWebsocketNetwork("MainNet Websocket", Chain.Ethereum),
            defaultHttpNetwork("MainNet HTTP", Chain.Ethereum),
//            defaultWebsocketNetwork("Ropsten", EthereumKit.NetworkType.EthRopsten),
//            defaultWebsocketNetwork("Rinkeby", EthereumKit.NetworkType.EthRinkeby),
//            defaultWebsocketNetwork("Kovan", EthereumKit.NetworkType.EthKovan),
//            defaultWebsocketNetwork("Goerli", EthereumKit.NetworkType.EthGoerli)
        )

    val binanceSmartChainNetworks: List<EvmNetwork>
        get() = listOfNotNull(
            defaultHttpNetwork("MainNet HTTP", Chain.BinanceSmartChain),
            defaultWebsocketNetwork("MainNet Websocket", Chain.BinanceSmartChain),
        )

    private fun defaultHttpSyncSource(chain: Chain): RpcSource? =
        when (chain) {
            Chain.Ethereum -> RpcSource.ethereumInfuraHttp(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            Chain.EthereumRopsten -> RpcSource.ropstenInfuraHttp(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            Chain.EthereumKovan -> RpcSource.kovanInfuraHttp(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            Chain.EthereumRinkeby -> RpcSource.rinkebyInfuraHttp(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            Chain.EthereumGoerli -> RpcSource.goerliInfuraHttp(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            Chain.BinanceSmartChain -> RpcSource.binanceSmartChainHttp()
            Chain.Polygon -> RpcSource.polygonRpcHttp()
            else -> null
        }

    private fun defaultWebsocketSyncSource(chain: Chain): RpcSource? =
        when (chain) {
            Chain.Ethereum -> RpcSource.ethereumInfuraWebSocket(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            Chain.EthereumRopsten -> RpcSource.ropstenInfuraWebSocket(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            Chain.EthereumKovan -> RpcSource.kovanInfuraWebSocket(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            Chain.EthereumRinkeby -> RpcSource.rinkebyInfuraWebSocket(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            Chain.EthereumGoerli -> RpcSource.goerliInfuraWebSocket(appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            Chain.BinanceSmartChain -> RpcSource.binanceSmartChainWebSocket()
            else -> null
        }

    private fun network(name: String, chain: Chain, rpcSource: RpcSource?): EvmNetwork? {
        if (rpcSource == null) return null

        return EvmNetwork(name, chain, rpcSource)
    }

    private fun defaultHttpNetwork(name: String, chain: Chain): EvmNetwork? {
        return network(name, chain, defaultHttpSyncSource(chain))
    }

    private fun defaultWebsocketNetwork(name: String, chain: Chain): EvmNetwork? {
        return network(name, chain, defaultWebsocketSyncSource(chain))
    }

}