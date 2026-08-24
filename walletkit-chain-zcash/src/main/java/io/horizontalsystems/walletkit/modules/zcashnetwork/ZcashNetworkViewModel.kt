package io.horizontalsystems.walletkit.modules.zcashnetwork

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.EndpointPingResult
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.ZcashEndpoint
import io.horizontalsystems.walletkit.uiv3.components.PingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ZcashNetworkViewModel(
    private val endpointManager: ZcashLightWalletEndpointManager
) : ViewModelUiState<ZcashNetworkViewModel.ViewState>() {

    val title = "Zcash"

    private var pingStates: Map<String, PingState> = emptyMap()
    private var pingJob: Job? = null

    override fun createState(): ViewState {
        val selectedEndpoint = endpointManager.currentEndpoint
        return ViewState(
            autoSelectEnabled = endpointManager.autoSelectEnabled,
            defaultItems = viewItems(endpointManager.defaultEndpoints, selectedEndpoint),
            customItems = viewItems(endpointManager.customEndpoints, selectedEndpoint)
        )
    }

    init {
        viewModelScope.launch {
            try {
                endpointManager.endpointsUpdatedFlow.collect {
                    pingEndpoints()
                }
            } catch (e: Exception) {
                // endpointsUpdatedFlow is a MutableSharedFlow and does not throw in normal operation
            }
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

    fun refresh() {
        pingEndpoints()
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
        if (!result.isValid || result.responseTime >= Double.MAX_VALUE) {
            return PingState.Unreachable
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
    )

    companion object {
        // gRPC over TLS is slower than Monero's plain JSON-RPC ping, so the badge thresholds are
        // scaled up from the Monero ones (333/667 ms) to keep the colour coding meaningful.
        private const val PING_GOOD = 500.0    // ms
        private const val PING_MEDIUM = 1000.0 // ms
    }
}
