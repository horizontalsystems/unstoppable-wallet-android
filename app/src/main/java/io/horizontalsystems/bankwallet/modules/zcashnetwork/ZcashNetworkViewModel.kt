package io.horizontalsystems.bankwallet.modules.zcashnetwork

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ZcashLightWalletEndpointManager
import io.horizontalsystems.bankwallet.core.managers.ZcashLightWalletEndpointManager.ZcashEndpoint
import kotlinx.coroutines.launch

class ZcashNetworkViewModel(
    private val endpointManager: ZcashLightWalletEndpointManager
) : ViewModelUiState<ZcashNetworkViewModel.ViewState>() {

    val title = "Zcash"

    override fun createState(): ViewState {
        val selectedEndpoint = endpointManager.currentEndpoint
        return ViewState(
            defaultItems = viewItems(endpointManager.defaultEndpoints, selectedEndpoint),
            customItems = viewItems(endpointManager.customEndpoints, selectedEndpoint)
        )
    }

    init {
        viewModelScope.launch {
            try {
                endpointManager.endpointsUpdatedFlow.collect {
                    emitState()
                }
            } catch (e: Exception) {
                // endpointsUpdatedFlow is a MutableSharedFlow and does not throw in normal operation
            }
        }
    }

    private fun viewItems(endpoints: List<ZcashEndpoint>, selectedEndpoint: ZcashEndpoint) =
        endpoints.map { endpoint ->
            ViewItem(
                endpoint = endpoint,
                id = endpoint.url,
                name = endpoint.name,
                url = endpoint.url,
                selected = endpoint == selectedEndpoint
            )
        }

    fun onSelectEndpoint(endpoint: ZcashEndpoint) {
        if (endpointManager.currentEndpoint == endpoint) return
        endpointManager.save(endpoint)
        emitState()
    }

    fun onRemoveCustomEndpoint(endpoint: ZcashEndpoint) {
        endpointManager.delete(endpoint)
        emitState()
    }

    data class ViewItem(
        val endpoint: ZcashEndpoint,
        val id: String,
        val name: String,
        val url: String,
        val selected: Boolean,
    )

    data class ViewState(
        val defaultItems: List<ViewItem>,
        val customItems: List<ViewItem>,
    )
}
