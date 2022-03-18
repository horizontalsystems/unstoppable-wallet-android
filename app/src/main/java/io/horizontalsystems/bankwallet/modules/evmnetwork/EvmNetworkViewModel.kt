package io.horizontalsystems.bankwallet.modules.evmnetwork

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.urls
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class EvmNetworkViewModel(private val service: EvmNetworkService) : ViewModel() {

    private val disposables = CompositeDisposable()

    val viewItemsLiveData = MutableLiveData<List<ViewItem>>()
    val finishLiveEvent = SingleLiveEvent<Unit>()

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
        val viewItems = items.map { viewItem(it) }.sortedBy { it.name }
        viewItemsLiveData.postValue(viewItems)
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
        finishLiveEvent.postValue(Unit)
    }

    override fun onCleared() {
        service.clear()
        disposables.clear()
    }

    data class SectionViewItem(
        val title: String,
        val viewItems: List<ViewItem>,
        val description: String?
    )

    data class ViewItem(
        val id: String,
        val name: String,
        val url: String,
        val selected: Boolean,
    )
}
