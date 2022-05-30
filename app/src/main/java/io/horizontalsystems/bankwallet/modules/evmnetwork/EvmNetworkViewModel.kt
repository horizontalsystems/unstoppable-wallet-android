package io.horizontalsystems.bankwallet.modules.evmnetwork

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.urls
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class EvmNetworkViewModel(private val service: EvmNetworkService) : ViewModel() {

    private val disposables = CompositeDisposable()

    var closeScreen by mutableStateOf(false)
        private set

    var viewItems by mutableStateOf<List<ViewItem>>(listOf())
        private set

    init {
        service.itemsObservable
            .subscribeIO {
                sync(it)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun sync(items: List<EvmNetworkService.Item>) {
        viewModelScope.launch {
            viewItems = items.map { viewItem(it) }.sortedBy { it.name }
        }
    }

    private fun viewItem(item: EvmNetworkService.Item): ViewItem {
        val url = if (item.syncSource.rpcSource.urls.size == 1)
            item.syncSource.rpcSource.urls.first().toString()
        else
            Translator.getString(R.string.NetworkSettings_SwithesAutomatically)

        return ViewItem(
            item.syncSource.id,
            item.syncSource.name,
            url,
            item.selected
        )
    }

    val title: String =
        service.blockchain.name

    fun onSelectViewItem(viewItem: ViewItem) {
        service.setCurrentNetwork(viewItem.id)
        closeScreen = true
    }

    override fun onCleared() {
        service.clear()
        disposables.clear()
    }

    data class ViewItem(
        val id: String,
        val name: String,
        val url: String,
        val selected: Boolean,
    )
}
