package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nearkit.network.Network
import io.horizontalsystems.walletkit.core.storage.BlockchainSettingsStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.URL

data class NearRpcSource(val name: String, val url: String)

/**
 * The user's NEAR JSON-RPC provider choice, persisted like XRP's. It sets the node for account
 * reads and transactions; token balances and history come from the FastNEAR indexer either way.
 */
class NearRpcSourceManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val marketKitWrapper: MarketKitWrapper,
) {
    private val blockchainType = BlockchainType.Near

    // extraBufferCapacity = 1 lets the non-suspend save() tryEmit without an active collector.
    private val _rpcSourceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val rpcSourceUpdatedFlow: SharedFlow<Unit> = _rpcSourceUpdatedFlow.asSharedFlow()

    val allRpcSources: List<NearRpcSource> = sources

    // Persisted by name under the shared "evm-sync-source-url" key, keyed by blockchain uid,
    // the same reuse XrpRpcSourceManager and SolanaRpcSourceManager rely on.
    val rpcSource: NearRpcSource
        get() {
            val name = blockchainSettingsStorage.evmSyncSourceUrl(blockchainType)
            return allRpcSources.firstOrNull { it.name == name } ?: allRpcSources[0]
        }

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(blockchainType.uid)

    /** The selected provider first, the others behind it as the kit's failover list. */
    fun rpcUrls(): List<URL> {
        val selected = rpcSource
        val ordered = listOf(selected) + allRpcSources.filter { it.name != selected.name }
        return ordered.map { URL(it.url) }
    }

    fun save(rpcSource: NearRpcSource) {
        blockchainSettingsStorage.save(rpcSource.name, blockchainType)
        _rpcSourceUpdatedFlow.tryEmit(Unit)
    }

    companion object {
        private val names = mapOf(
            "free.rpc.fastnear.com" to "FastNEAR",
            "near.drpc.org" to "dRPC",
            "rpc.shitzuapes.xyz" to "Shitzu",
            "rpc.mainnet.near.org" to "NEAR.org",
        )

        // [0] is the default: the kit's built-in MainNet failover order, so the lists cannot drift
        val sources = Network.MainNet.rpcUrls.map { url ->
            NearRpcSource(names[url.host] ?: url.host, url.toString())
        }
    }
}
