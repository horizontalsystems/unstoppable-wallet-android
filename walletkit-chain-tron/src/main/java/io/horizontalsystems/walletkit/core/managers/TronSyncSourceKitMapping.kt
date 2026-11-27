package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.tronkit.models.RpcSource
import io.horizontalsystems.tronkit.network.Network
import io.horizontalsystems.walletkit.entities.EvmSyncSource
import java.net.URL

// Maps the kit-free sync-source entity to tron-kit types at the kit boundary.

private const val TRON_GRID_URL = "https://api.trongrid.io/"

val EvmSyncSource.isTronGrid: Boolean
    get() = uri.toString() == TRON_GRID_URL

/**
 * One source over the URLs of the given sources, in order; tron-kit falls over to the next URL
 * when one fails and sticks with the one that answered. The sources belong to different
 * providers, so each URL carries its own credentials: the TronGrid API keys go to TronGrid
 * only, and a source's auth to that source only.
 */
fun List<EvmSyncSource>.tronRpcSource(network: Network, tronGridApiKeys: List<String>): RpcSource =
    RpcSource(flatMap { it.tronEndpoints(network, tronGridApiKeys) })

private fun EvmSyncSource.tronEndpoints(network: Network, tronGridApiKeys: List<String>): List<RpcSource.Endpoint> =
    if (isTronGrid) {
        listOf(RpcSource.Endpoint(network.tronGridUrl, apiKeys = tronGridApiKeys))
    } else {
        uris.map { RpcSource.Endpoint(URL(it.toString()), auth = auth) }
    }
