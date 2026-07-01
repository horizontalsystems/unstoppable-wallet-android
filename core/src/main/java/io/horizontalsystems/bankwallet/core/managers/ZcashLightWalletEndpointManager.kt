package io.horizontalsystems.bankwallet.core.managers

import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.ZcashEndpointStorage
import io.horizontalsystems.bankwallet.entities.ZcashEndpointRecord
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.URI

class ZcashLightWalletEndpointManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val endpointStorage: ZcashEndpointStorage,
    private val marketKitWrapper: MarketKitWrapper,
) {
    private val _currentEndpointUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val currentEndpointUpdatedFlow = _currentEndpointUpdatedFlow.asSharedFlow()

    private val _endpointsUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val endpointsUpdatedFlow = _endpointsUpdatedFlow.asSharedFlow()

    val defaultEndpointsInitial = listOf(
        ZcashEndpoint("https://zec.rocks:443", "zec.rocks"),
        ZcashEndpoint("https://na.zec.rocks:443", "na.zec.rocks"),
        ZcashEndpoint("https://sa.zec.rocks:443", "sa.zec.rocks"),
        ZcashEndpoint("https://eu.zec.rocks:443", "eu.zec.rocks"),
        ZcashEndpoint("https://ap.zec.rocks:443", "ap.zec.rocks"),
        ZcashEndpoint("https://us.zec.stardust.rest:443", "us.zec.stardust.rest"),
        ZcashEndpoint("https://eu.zec.stardust.rest:443", "eu.zec.stardust.rest"),
        ZcashEndpoint("https://eu2.zec.stardust.rest:443", "eu2.zec.stardust.rest"),
        ZcashEndpoint("https://jp.zec.stardust.rest:443", "jp.zec.stardust.rest"),
    )

    val defaultEndpoints: List<ZcashEndpoint> get() = defaultEndpointsInitial

    val customEndpoints: List<ZcashEndpoint>
        get() {
            val defaultUrls = defaultEndpointsInitial.map { it.url }
            return endpointStorage.getAll()
                .filterNot { defaultUrls.contains(it.url) }
                .map { ZcashEndpoint(it.url, it.url) }
        }

    val allEndpoints: List<ZcashEndpoint>
        get() = defaultEndpoints + customEndpoints

    val currentEndpoint: ZcashEndpoint
        get() {
            val url = blockchainSettingsStorage.zcashEndpointUrl()
            return allEndpoints.firstOrNull { it.url == url } ?: defaultEndpoints.first()
        }

    val currentLightWalletEndpoint: LightWalletEndpoint
        get() = currentEndpoint.toLightWalletEndpoint()

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(BlockchainType.Zcash.uid)

    fun save(endpoint: ZcashEndpoint) {
        blockchainSettingsStorage.saveZcashEndpoint(endpoint.url)
        _currentEndpointUpdatedFlow.tryEmit(endpoint.url)
    }

    fun addCustomEndpoint(url: String) {
        endpointStorage.save(ZcashEndpointRecord(url))
        customEndpoints.firstOrNull { it.url == url }?.let { save(it) }
        _endpointsUpdatedFlow.tryEmit(url)
    }

    fun delete(endpoint: ZcashEndpoint) {
        val isCurrent = endpoint == currentEndpoint
        endpointStorage.delete(endpoint.url)
        if (isCurrent) {
            save(defaultEndpoints.first())
        }
        _endpointsUpdatedFlow.tryEmit(endpoint.url)
    }

    data class ZcashEndpoint(val url: String, val name: String) {
        fun toLightWalletEndpoint(): LightWalletEndpoint {
            val uri = URI(url)
            return LightWalletEndpoint(
                host = uri.host ?: url,
                port = uri.port.takeIf { it > 0 } ?: 443,
                isSecure = uri.scheme?.lowercase() == "https"
            )
        }
    }
}
