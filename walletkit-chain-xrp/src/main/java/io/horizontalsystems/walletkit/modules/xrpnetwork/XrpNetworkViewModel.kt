package io.horizontalsystems.walletkit.modules.xrpnetwork

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.core.ViewModelUiState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

class XrpNetworkViewModel(private val service: XrpNetworkService) : ViewModelUiState<XrpNetworkViewModel.UiState>() {

    private var closeScreen = false
    private var viewItems = listOf<ViewItem>()

    val title: String = "XRP Ledger"
    val blockchainType = BlockchainType.Xrp

    override fun createState() = UiState(
        closeScreen = closeScreen,
        viewItems = viewItems,
    )

    init {
        viewModelScope.launch {
            service.stateFlow
                .catch { Timber.e(it, "XRP network items collection failed") }
                .collect { state ->
                    viewItems = state.items.map { viewItem(it) }
                    emitState()
                }
        }
    }

    private fun viewItem(item: XrpNetworkService.Item) = ViewItem(
        name = item.rpcSource.name,
        url = item.rpcSource.url,
        selected = item.selected,
    )

    fun onSelectViewItem(viewItem: ViewItem) {
        service.setCurrentSource(viewItem.name)
        closeScreen = true
        emitState()
    }

    override fun onCleared() {
        service.clear()
    }

    data class UiState(
        val closeScreen: Boolean,
        val viewItems: List<ViewItem>,
    )

    data class ViewItem(
        val name: String,
        val url: String,
        val selected: Boolean,
    )
}
