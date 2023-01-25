package io.horizontalsystems.bankwallet.modules.solananetwork

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class SolanaNetworkViewModel(private val service: SolanaNetworkService) : ViewModel() {

    private val disposables = CompositeDisposable()

    var closeScreen by mutableStateOf(false)
        private set

    var viewItems by mutableStateOf<List<ViewItem>>(listOf())
        private set

    val title: String = "Solana"
    val blockchainType = BlockchainType.Solana

    init {
        service.itemsObservable
            .subscribeIO {
                sync(it)
            }
            .let {
                disposables.add(it)
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
        disposables.clear()
    }

    data class ViewItem(
        val name: String,
        val url: String,
        val selected: Boolean
    )
}
