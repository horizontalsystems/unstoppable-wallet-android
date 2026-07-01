package io.horizontalsystems.bankwallet.modules.solananetwork

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class SolanaNetworkViewModel(private val service: SolanaNetworkService) : ViewModel() {

    var closeScreen by mutableStateOf(false)
        private set

    var viewItems by mutableStateOf<List<ViewItem>>(listOf())
        private set

    val title: String = "Solana"
    val blockchainType = BlockchainType.Solana

    init {
        viewModelScope.launch {
            service.itemsObservable.asFlow().collect {
                sync(it)
            }
        }
    }

    private fun sync(items: List<SolanaNetworkService.Item>) {
        viewModelScope.launch {
            viewItems = items.map { viewItem(it) }.sortedBy { it.name }
        }
    }

    private fun viewItem(item: SolanaNetworkService.Item): ViewItem {
        val url = item.rpcSource.url.toString()

        return ViewItem(
            item.rpcSource.name,
            url,
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
