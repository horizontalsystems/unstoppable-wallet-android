package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.URL

data class ThorchainRpcSource(val name: String, val url: String)

class ThorchainRpcSourceManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val marketKitWrapper: MarketKitWrapper,
) {

    private val blockchainType = BlockchainType.Thorchain

    // extraBufferCapacity = 1 lets the non-suspend save() tryEmit without an active collector,
    // matching the fire-and-forget change signal PublishSubject provided.
    private val _rpcSourceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val rpcSourceUpdatedFlow: SharedFlow<Unit> = _rpcSourceUpdatedFlow.asSharedFlow()

    // Displayed/stored URLs carry no secret. The Liquify entry shows its public path; the
    // API key is injected only at request time (see thornodeUrl), mirroring how
    // TronKitManager swaps in the TronGrid key only for the TronGrid source.
    // Order matters: allRpcSources[0] is the default, and the failover list (thornodeUrls)
    // tries the selected source first, then the rest in this order. Keplr (a reputable,
    // established operator) leads; Liquify is last as it returns intermittent 500s.
    val allRpcSources = listOf(
        ThorchainRpcSource("Keplr", "https://lcd-thorchain.keplr.app/"),
        ThorchainRpcSource("Rorcual", "https://api-thorchain.rorcual.xyz/"),
        ThorchainRpcSource("Liquify", LIQUIFY_PUBLIC_URL),
    )

    // The selection is persisted by name under the shared "evm-sync-source-url" key, keyed
    // by the Thorchain blockchain uid — the same reuse SolanaRpcSourceManager relies on.
    val rpcSource: ThorchainRpcSource
        get() {
            val name = blockchainSettingsStorage.evmSyncSourceUrl(blockchainType)
            return allRpcSources.firstOrNull { it.name == name } ?: allRpcSources[0]
        }

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(blockchainType.uid)

    // Failover list for the kit: the selected source first (it serves ~all traffic), then the
    // remaining providers as backup. The kit's ThornodeApiProvider.withFailover retries the next
    // URL on 5xx/network errors, so a single provider's intermittent 500s no longer break sync.
    // Every entry ends in "/" so the kit's Retrofit baseUrl accepts it.
    fun thornodeUrls(): List<URL> {
        val selected = rpcSource
        val ordered = listOf(selected) + allRpcSources.filter { it.name != selected.name }
        return ordered.map { resolvedUrl(it) }
    }

    // The Liquify entry shows a public path but is keyed with the app API key at request time.
    private fun resolvedUrl(source: ThorchainRpcSource): URL =
        if (source.url == LIQUIFY_PUBLIC_URL) {
            URL("https://gateway.liquify.com/api=${App.appConfigProvider.thorchainApiKey}/thorchain_api/")
        } else {
            URL(source.url)
        }

    fun save(rpcSource: ThorchainRpcSource) {
        blockchainSettingsStorage.save(rpcSource.name, blockchainType)
        _rpcSourceUpdatedFlow.tryEmit(Unit)
    }

    companion object {
        private const val LIQUIFY_PUBLIC_URL = "https://gateway.liquify.com/chain/thorchain_api/"
    }
}
