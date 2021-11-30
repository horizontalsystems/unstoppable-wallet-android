package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.EvmNetwork
import io.horizontalsystems.ethereumkit.core.EthereumKit

class EvmNetworkManager(private val appConfigProvider: AppConfigProvider) {

    val ethereumNetworks: List<EvmNetwork>
        get() = listOfNotNull(
            defaultWebsocketNetwork("MainNet Websocket", EthereumKit.NetworkType.EthMainNet),
            defaultHttpNetwork("MainNet HTTP", EthereumKit.NetworkType.EthMainNet),
//            defaultWebsocketNetwork("Ropsten", EthereumKit.NetworkType.EthRopsten),
//            defaultWebsocketNetwork("Rinkeby", EthereumKit.NetworkType.EthRinkeby),
//            defaultWebsocketNetwork("Kovan", EthereumKit.NetworkType.EthKovan),
//            defaultWebsocketNetwork("Goerli", EthereumKit.NetworkType.EthGoerli)
        )

    val binanceSmartChainNetworks: List<EvmNetwork>
        get() = listOfNotNull(
            defaultHttpNetwork("MainNet HTTP", EthereumKit.NetworkType.BscMainNet),
            defaultWebsocketNetwork("MainNet Websocket", EthereumKit.NetworkType.BscMainNet),
        )

    private fun defaultHttpSyncSource(networkType: EthereumKit.NetworkType): EthereumKit.SyncSource? =
        when (networkType) {
            EthereumKit.NetworkType.EthMainNet,
            EthereumKit.NetworkType.EthRopsten,
            EthereumKit.NetworkType.EthKovan,
            EthereumKit.NetworkType.EthRinkeby,
            EthereumKit.NetworkType.EthGoerli -> EthereumKit.infuraHttpSyncSource(networkType, appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            EthereumKit.NetworkType.BscMainNet -> EthereumKit.defaultBscHttpSyncSource()
        }

    private fun defaultWebsocketSyncSource(networkType: EthereumKit.NetworkType): EthereumKit.SyncSource? =
        when (networkType) {
            EthereumKit.NetworkType.EthMainNet,
            EthereumKit.NetworkType.EthRopsten,
            EthereumKit.NetworkType.EthKovan,
            EthereumKit.NetworkType.EthRinkeby,
            EthereumKit.NetworkType.EthGoerli -> EthereumKit.infuraWebSocketSyncSource(networkType, appConfigProvider.infuraProjectId, appConfigProvider.infuraProjectSecret)
            EthereumKit.NetworkType.BscMainNet -> EthereumKit.defaultBscWebSocketSyncSource()
        }

    private fun network(name: String, networkType: EthereumKit.NetworkType, syncSource: EthereumKit.SyncSource?): EvmNetwork? {
        if (syncSource == null) return null

        return EvmNetwork(name, networkType, syncSource)
    }

    private fun defaultHttpNetwork(name: String, networkType: EthereumKit.NetworkType): EvmNetwork? {
        return network(name, networkType, defaultHttpSyncSource(networkType))
    }

    private fun defaultWebsocketNetwork(name: String, networkType: EthereumKit.NetworkType): EvmNetwork? {
        return network(name, networkType, defaultWebsocketSyncSource(networkType))
    }

}