package io.horizontalsystems.walletkit.modules.xrpnetwork

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

class XrpNetworkViewModel(private val service: XrpNetworkService) : ViewModel() {

    var closeScreen by mutableStateOf(false)
        private set

    var viewItems by mutableStateOf<List<ViewItem>>(listOf())
        private set

    val title: String = "XRP Ledger"
    val blockchainType = BlockchainType.Xrp

    init {
        viewModelScope.launch {
            service.itemsFlow
                .catch { Timber.e(it, "XRP network items collection failed") }
                .collect {
                    sync(it)
                }
        }
    }

    private fun sync(items: List<XrpNetworkService.Item>) {
        viewModelScope.launch {
            viewItems = items.map { viewItem(it) }
        }
    }

    private fun viewItem(item: XrpNetworkService.Item): ViewItem {
        return ViewItem(
            item.rpcSource.name,
            item.rpcSource.url,
            item.selected
        )
    }

    fun onSelectViewItem(viewItem: ViewItem) {
        service.setCurrentSource(viewItem.name)
        closeScreen = true
    }

    override fun onCleared() {
        service.clear()
    }

    data class ViewItem(
        val name: String,
        val url: String,
        val selected: Boolean
    )
}
