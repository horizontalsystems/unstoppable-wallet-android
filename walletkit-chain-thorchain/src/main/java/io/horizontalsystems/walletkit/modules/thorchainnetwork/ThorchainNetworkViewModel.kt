package io.horizontalsystems.walletkit.modules.thorchainnetwork

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

class ThorchainNetworkViewModel(private val service: ThorchainNetworkService) : ViewModel() {

    var closeScreen by mutableStateOf(false)
        private set

    var viewItems by mutableStateOf<List<ViewItem>>(listOf())
        private set

    val title: String = "THORChain"
    val blockchainType = BlockchainType.Thorchain

    init {
        viewModelScope.launch {
            service.itemsFlow
                .catch { Timber.e(it, "Thorchain network items collection failed") }
                .collect {
                    sync(it)
                }
        }
    }

    private fun sync(items: List<ThorchainNetworkService.Item>) {
        viewModelScope.launch {
            viewItems = items.map { viewItem(it) }
        }
    }

    private fun viewItem(item: ThorchainNetworkService.Item): ViewItem {
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
