package io.horizontalsystems.walletkit.modules.zcashnetwork

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.EndpointPingResult
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.ZcashEndpoint
import io.horizontalsystems.walletkit.uiv3.components.PingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ZcashNetworkViewModel(
    private val endpointManager: ZcashLightWalletEndpointManager
) : ViewModelUiState<ZcashNetworkViewModel.ViewState>() {

    val title = "Zcash"

    private var pingStates: Map<String, PingState> = emptyMap()
    private var pingJob: Job? = null
    private var localNetworkUrls: Set<String> = emptySet()
    private var localNetworkJob: Job? = null

    override fun createState(): ViewState {
        val selectedEndpoint = endpointManager.currentEndpoint
        return ViewState(
            autoSelectEnabled = endpointManager.autoSelectEnabled,
            defaultItems = viewItems(endpointManager.defaultEndpoints, selectedEndpoint),
            customItems = viewItems(endpointManager.customEndpoints, selectedEndpoint),
            hasLocalNetworkEndpoint = localNetworkUrls.isNotEmpty(),
        )
    }

    init {
        viewModelScope.launch {
            try {
                endpointManager.endpointsUpdatedFlow.collect {
                    pingEndpoints()
                    findLocalNetworkEndpoint()
                }
            } catch (e: Exception) {
                // endpointsUpdatedFlow is a MutableSharedFlow and does not throw in normal operation
            }
        }
        pingEndpoints()
        findLocalNetworkEndpoint()
    }

    // Only custom endpoints can be local; the default ones are public servers.
    private fun findLocalNetworkEndpoint() {
        val urls = endpointManager.customEndpoints.map { it.url }

        // The job is replaced with the list, so a lookup started for an endpoint that has since
        // been removed cannot bring it back.
        localNetworkJob?.cancel()
        localNetworkUrls = localNetworkUrls.intersect(urls.toSet())
        emitState()

        localNetworkJob = viewModelScope.launch {
            // Each result is published as it arrives: one local endpoint is enough to ask for
            // access, and a name that is slow to resolve must not hold that back.
            urls.forEach { url ->
                launch {
                    // Removed as well as added: a name that was local on the last network may
                    // lead to a public address on this one.
                    localNetworkUrls = if (endpointManager.isLocalEndpoint(url)) {
                        localNetworkUrls + url
                    } else {
                        localNetworkUrls - url
                    }
                    emitState()
                }
            }
        }
    }

    /**
     * The system dropped every connection to a local endpoint until now, so the pings run again,
     * and a wallet that was started on such an endpoint gets a fresh synchronizer: the one it has
     * may have stopped for good. The manager classifies the current endpoint itself rather than
     * this view model looking it up in [localNetworkUrls], which may still be filling.
     */
    fun onLocalNetworkAccessGranted() {
        viewModelScope.launch {
            endpointManager.retryCurrentEndpointIfLocal()
        }
        pingEndpoints()
    }

    private fun viewItems(endpoints: List<ZcashEndpoint>, selectedEndpoint: ZcashEndpoint) =
        endpoints.map { endpoint ->
            ViewItem(
                endpoint = endpoint,
                id = endpoint.url,
                name = endpoint.name,
                url = endpoint.url,
                selected = endpoint == selectedEndpoint,
                ping = pingStates[endpoint.url] ?: PingState.Loading
            )
        }

    fun onSelectEndpoint(endpoint: ZcashEndpoint) {
        if (endpointManager.currentEndpoint == endpoint) return
        endpointManager.save(endpoint)
        emitState()
    }

    fun onToggleAutoSelect(enabled: Boolean) {
        endpointManager.autoSelectEnabled = enabled
        // re-ping; when enabled the fastest reachable endpoint is selected once results arrive
        pingEndpoints()
    }

    fun onRemoveCustomEndpoint(endpoint: ZcashEndpoint) {
        endpointManager.delete(endpoint)
        emitState()
    }

    // The lookup runs again as well: after a change of network a name may lead somewhere else.
    fun refresh() {
        pingEndpoints()
        findLocalNetworkEndpoint()
    }

    private fun pingEndpoints() {
        val endpoints = endpointManager.allEndpoints

        pingStates = endpoints.associate { it.url to PingState.Loading }
        emitState()

        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            pingStates = try {
                val results = endpointManager.pingEndpoints(endpoints.map { it.url })
                    .associateBy { it.url }
                endpoints.associate { endpoint ->
                    endpoint.url to (results[endpoint.url]?.let(::pingState) ?: PingState.Unreachable)
                }
            } catch (e: CancellationException) {
                // A newer ping has replaced this one and owns the states now.
                throw e
            } catch (e: Exception) {
                endpoints.associate { it.url to PingState.Unreachable }
            }
            if (endpointManager.autoSelectEnabled) {
                selectFastestEndpoint(endpoints)
            }
            emitState()
        }
    }

    private fun selectFastestEndpoint(endpoints: List<ZcashEndpoint>) {
        val fastest = endpoints
            .mapNotNull { endpoint ->
                (pingStates[endpoint.url] as? PingState.Reachable)?.let { endpoint to it.responseTimeMs }
            }
            .minByOrNull { it.second }
            ?.first
            ?: return

        if (fastest.url != endpointManager.currentEndpoint.url) {
            endpointManager.save(fastest)
        }
    }

    private fun pingState(result: EndpointPingResult): PingState {
        when (result.status) {
            EndpointPingResult.Status.Unreachable -> return PingState.Unreachable
            EndpointPingResult.Status.NotSynced -> return PingState.NotSynced
            EndpointPingResult.Status.Ok -> if (result.responseTime >= Double.MAX_VALUE) return PingState.Unreachable
        }
        val level = when {
            result.responseTime <= PING_GOOD -> PingState.Level.Good
            result.responseTime <= PING_MEDIUM -> PingState.Level.Medium
            else -> PingState.Level.Slow
        }
        return PingState.Reachable(result.responseTime.toInt(), level)
    }

    data class ViewItem(
        val endpoint: ZcashEndpoint,
        val id: String,
        val name: String,
        val url: String,
        val selected: Boolean,
        val ping: PingState,
    )

    data class ViewState(
        val autoSelectEnabled: Boolean,
        val defaultItems: List<ViewItem>,
        val customItems: List<ViewItem>,
        val hasLocalNetworkEndpoint: Boolean,
    )

    companion object {
        // gRPC over TLS is slower than Monero's plain JSON-RPC ping, so the badge thresholds are
        // scaled up from the Monero ones (333/667 ms) to keep the colour coding meaningful.
        private const val PING_GOOD = 500.0    // ms
        private const val PING_MEDIUM = 1000.0 // ms
    }
}
