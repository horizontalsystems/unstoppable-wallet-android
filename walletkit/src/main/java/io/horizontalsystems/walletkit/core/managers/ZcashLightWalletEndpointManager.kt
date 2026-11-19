package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.LocalNetworkAccess
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
import java.util.concurrent.ConcurrentHashMap
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

    /**
     * True when the wallet syncs through a server in the device's local network. Only a custom
     * endpoint can be one; the default endpoints are public servers.
     */
    suspend fun currentEndpointIsLocal(): Boolean {
        val current = currentEndpoint
        return current in customEndpoints && isLocalEndpoint(current.url)
    }

    /**
     * True when the endpoint at [url] is in the device's local network. Every screen that asks
     * for local network access classifies through here, so that what it found is remembered for
     * [retryCurrentEndpointIfLocal].
     */
    suspend fun isLocalEndpoint(url: String): Boolean {
        val isLocal = LocalNetworkAccess.classifyUrl(url)
        when (isLocal) {
            true -> knownLocalUrls.add(url)
            false -> knownLocalUrls.remove(url)
            // The lookup failed: what was known before still stands.
            null -> Unit
        }
        return isLocal == true
    }

    // Endpoints found to be local by their latest lookup that gave an answer. A lookup that fails
    // says nothing, so the recovery after an access grant must not hang on one more lookup of an
    // endpoint already known to be local: the grant was asked for because of it. An endpoint that
    // has since resolved to a public address is dropped, so it is not rebuilt for a grant that
    // another endpoint asked for.
    private val knownLocalUrls = ConcurrentHashMap.newKeySet<String>()

    /**
     * Rebuilds the wallet on its endpoint when that endpoint is local, for when local network
     * access has just been granted. Classifying the endpoint may take a name lookup, so the
     * selection is read again afterwards: an endpoint chosen in the meantime has already been
     * started by its own selection.
     */
    suspend fun retryCurrentEndpointIfLocal() {
        val endpoint = currentEndpoint
        if (endpoint !in customEndpoints) return
        // Read again after the lookup: while it ran, or failed, a screen's own lookup of the same
        // endpoint may have found it local.
        val isLocal = endpoint.url in knownLocalUrls || isLocalEndpoint(endpoint.url) || endpoint.url in knownLocalUrls
        if (!isLocal) return
        if (currentEndpoint == endpoint) retryCurrentEndpoint()
    }

    var autoSelectEnabled: Boolean
        get() = blockchainSettingsStorage.zcashAutoSelect()
        set(value) {
            blockchainSettingsStorage.saveZcashAutoSelect(value)
        }

    /**
     * Pings lightwalletd endpoints and reports reachability/latency. Supplied by the Zcash chain
     * plugin (the implementation lives in walletkit-chain-zcash); null while the module is absent.
     */
    @Volatile
    var endpointPinger: (suspend (urls: List<String>) -> List<EndpointPingResult>)? = null

    suspend fun pingEndpoints(urls: List<String>): List<EndpointPingResult> =
        endpointPinger?.invoke(urls) ?: emptyList()

    suspend fun autoSelectFastestEndpointOnStartup() {
        if (!autoSelectEnabled || endpointPinger == null) return

        try {
            // The adapter starts on the stored endpoint right away — deferring its creation until
            // the probe finished left wallet rows interactive with no adapter behind them, which
            // crashed the send/shield/migration screens. Capped so a slow gRPC/TLS handshake
            // cannot stall the startup gate.
            withTimeoutOrNull(STARTUP_PING_TIMEOUT) {
                val fastest = pickFastest() ?: return@withTimeoutOrNull
                // save() emits, so a change rebuilds the adapter once onto the winner; when the
                // stored endpoint is still the fastest nothing is emitted and nothing reconnects.
                if (fastest.url != currentEndpoint.url) save(fastest)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // keep the stored endpoint on any ping failure
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
        val status: Status,
        val responseTime: Double,
    ) {
        val isValid: Boolean get() = status == Status.Ok

        enum class Status {
            Ok,

            /** No usable answer: connection, TLS or request failure, or a server of another network. */
            Unreachable,

            /** The server answered but its chain is behind, so a wallet on it would not sync to the tip. */
            NotSynced,
        }
    }

    data class ZcashEndpoint(val url: String, val name: String) {
    }

    companion object {
        private val STARTUP_PING_TIMEOUT = 8.seconds
    }
}
