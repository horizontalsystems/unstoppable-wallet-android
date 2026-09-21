package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.URL

data class XrpRpcSource(val name: String, val url: String)

/** The user's XRPL JSON-RPC provider choice, persisted like Solana's and THORChain's. */
class XrpRpcSourceManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val marketKitWrapper: MarketKitWrapper,
) {
    private val blockchainType = BlockchainType.Xrp

    // extraBufferCapacity = 1 lets the non-suspend save() tryEmit without an active collector.
    private val _rpcSourceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val rpcSourceUpdatedFlow: SharedFlow<Unit> = _rpcSourceUpdatedFlow.asSharedFlow()

    val allRpcSources: List<XrpRpcSource> = sources

    // Persisted by name under the shared "evm-sync-source-url" key, keyed by blockchain uid,
    // the same reuse SolanaRpcSourceManager and ThorchainRpcSourceManager rely on.
    val rpcSource: XrpRpcSource
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

    fun save(rpcSource: XrpRpcSource) {
        blockchainSettingsStorage.save(rpcSource.name, blockchainType)
        _rpcSourceUpdatedFlow.tryEmit(Unit)
    }

    companion object {
        // [0] is the default. Mirrors the kit's built-in MainNet failover order.
        val sources = listOf(
            XrpRpcSource("XRPL Cluster", "https://xrplcluster.com/"),
            XrpRpcSource("Ripple s2", "https://s2.ripple.com:51234/"),
            XrpRpcSource("Ripple s1", "https://s1.ripple.com:51234/"),
        )
    }
}
