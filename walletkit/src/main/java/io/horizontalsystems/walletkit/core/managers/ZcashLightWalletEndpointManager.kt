package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.walletkit.core.storage.ZcashEndpointStorage
import io.horizontalsystems.walletkit.entities.ZcashEndpointRecord
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class ZcashLightWalletEndpointManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val endpointStorage: ZcashEndpointStorage,
    private val marketKitWrapper: MarketKitWrapper,
) {
    private val reselectMutex = Mutex()

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

    var autoSelectEnabled: Boolean
        get() = blockchainSettingsStorage.zcashAutoSelect()
        set(value) {
            blockchainSettingsStorage.saveZcashAutoSelect(value)
        }

    // True while the startup ping is choosing the fastest endpoint. The Zcash adapter creation is
    // deferred while this is set, so the wallet connects once to the fastest server instead of
    // connecting to the stored one and then reconnecting. Set at construction (before adapters
    // are initialized) to avoid a race.
    @Volatile
    var isResolvingFastestEndpoint: Boolean = autoSelectEnabled
        private set

    /**
     * Pings lightwalletd endpoints and reports reachability/latency. Supplied by the Zcash chain
     * plugin (the implementation lives in walletkit-chain-zcash); null while the module is absent.
     */
    @Volatile
    var endpointPinger: (suspend (urls: List<String>) -> List<EndpointPingResult>)? = null

    suspend fun pingEndpoints(urls: List<String>): List<EndpointPingResult> =
        endpointPinger?.invoke(urls) ?: emptyList()

    suspend fun autoSelectFastestEndpointOnStartup() {
        if (!autoSelectEnabled || endpointPinger == null) {
            isResolvingFastestEndpoint = false
            return
        }

        var target = currentEndpoint
        try {
            // Adapter creation is blocked until this returns, so cap the whole probe: a gRPC/TLS
            // handshake per endpoint is slower than a plain HTTP ping and must not stall startup.
            withTimeoutOrNull(STARTUP_PING_TIMEOUT) {
                pickFastest()?.let { target = it }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // keep the stored endpoint on any ping failure
        } finally {
            // Persist WITHOUT emitting currentEndpointUpdatedFlow: emitting would replay (replay=1)
            // into WalletManager's late collector and trigger reloadWallets(Zcash) -> adapter
            // teardown/reconnect churn. The adapter is (re)created once by the normal wallet
            // activation / WalletManager.refreshActiveWallets() with this endpoint already current.
            persist(target)
            isResolvingFastestEndpoint = false
        }
    }

    /**
     * Re-runs the probe and switches to the fastest reachable endpoint, for when the current one
     * has died while the app was running. Unlike the startup path this emits, so the adapter is
     * rebuilt on the new endpoint.
     *
     * @return true when the endpoint actually changed.
     */
    suspend fun reselectFastestEndpoint(): Boolean {
        if (!autoSelectEnabled || endpointPinger == null) return false
        if (!reselectMutex.tryLock()) return false // a probe is already in flight

        return try {
            val fastest = withTimeoutOrNull(STARTUP_PING_TIMEOUT) { pickFastest() } ?: return false
            // Every endpoint failing means the device has no usable network, not that the current
            // one is bad — pickFastest returns null there, so nothing is switched.
            if (fastest.url == currentEndpoint.url) return false
            save(fastest)
            true
        } finally {
            reselectMutex.unlock()
        }
    }

    /**
     * Re-emits the current endpoint so the wallet reload rebuilds the adapter on it. For a stalled
     * sync where reselection found nothing better — the current server may answer pings yet fail
     * block downloads, leaving the synchronizer terminally STOPPED — a fresh synchronizer is the
     * only recovery.
     */
    fun retryCurrentEndpoint() {
        _currentEndpointUpdatedFlow.tryEmit(currentEndpoint.url)
    }

    /** Pings every endpoint and returns the fastest valid one, or null if none responded. */
    private suspend fun pickFastest(): ZcashEndpoint? {
        val endpoints = allEndpoints
        val results = pingEndpoints(endpoints.map { it.url }).associateBy { it.url }

        return endpoints
            .mapNotNull { endpoint ->
                results[endpoint.url]
                    ?.takeIf { it.isValid && it.responseTime < Double.MAX_VALUE }
                    ?.let { endpoint to it.responseTime }
            }
            .minByOrNull { it.second }
            ?.first
    }

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(BlockchainType.Zcash.uid)

    fun save(endpoint: ZcashEndpoint) {
        persist(endpoint)
        _currentEndpointUpdatedFlow.tryEmit(endpoint.url)
    }

    private fun persist(endpoint: ZcashEndpoint) {
        blockchainSettingsStorage.saveZcashEndpoint(endpoint.url)
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

    data class EndpointPingResult(
        val url: String,
        val isValid: Boolean,
        val responseTime: Double,
    )

    data class ZcashEndpoint(val url: String, val name: String) {
    }

    companion object {
        private val STARTUP_PING_TIMEOUT = 8.seconds
    }
}
