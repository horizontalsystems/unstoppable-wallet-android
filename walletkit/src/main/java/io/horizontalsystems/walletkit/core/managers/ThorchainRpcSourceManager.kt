package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.net.URL

data class ThorchainRpcSource(val name: String, val url: String)

class ThorchainRpcSourceManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val marketKitWrapper: MarketKitWrapper,
) {

    private val blockchainType = BlockchainType.Thorchain
    private val rpcSourceSubjectUpdate = PublishSubject.create<Unit>()

    val rpcSourceUpdateObservable: Observable<Unit>
        get() = rpcSourceSubjectUpdate

    // Displayed/stored URLs carry no secret. The Liquify entry shows its public path; the
    // API key is injected only at request time (see thornodeUrl), mirroring how
    // TronKitManager swaps in the TronGrid key only for the TronGrid source.
    val allRpcSources = listOf(
        ThorchainRpcSource("Liquify", LIQUIFY_PUBLIC_URL),
        ThorchainRpcSource("Keplr", "https://lcd-thorchain.keplr.app/"),
        ThorchainRpcSource("Rorcual", "https://api-thorchain.rorcual.xyz/"),
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

    // Resolved thornode base URL for the selected source, with the Liquify key applied.
    // Every entry ends in "/" so the kit's Retrofit baseUrl accepts it.
    fun thornodeUrl(): URL {
        val source = rpcSource
        return if (source.url == LIQUIFY_PUBLIC_URL) {
            URL("https://gateway.liquify.com/api=${App.appConfigProvider.thorchainApiKey}/thorchain_api/")
        } else {
            URL(source.url)
        }
    }

    fun save(rpcSource: ThorchainRpcSource) {
        blockchainSettingsStorage.save(rpcSource.name, blockchainType)
        rpcSourceSubjectUpdate.onNext(Unit)
    }

    companion object {
        private const val LIQUIFY_PUBLIC_URL = "https://gateway.liquify.com/chain/thorchain_api/"
    }
}
