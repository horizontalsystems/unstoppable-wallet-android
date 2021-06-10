package io.horizontalsystems.bankwallet.modules.evmnetwork

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.url
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class EvmNetworkViewModel(private val service: EvmNetworkService): ViewModel() {

    private val disposables = CompositeDisposable()

    val sectionViewItemsLiveData = MutableLiveData<List<SectionViewItem>>()
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
        val (mainNetItems, testNetItems) = items.partition { it.isMainNet }

        val sectionViewItems: List<SectionViewItem> = listOfNotNull(
            sectionViewItem("MainNet", mainNetItems.map { viewItem(it) }),
            sectionViewItem("TestNet", testNetItems.map { viewItem(it) })
        )

        sectionViewItemsLiveData.postValue(sectionViewItems)
    }

    private fun sectionViewItem(title: String, viewItems: List<ViewItem>): SectionViewItem? {
        if (viewItems.isEmpty()) return null

        return SectionViewItem(title, viewItems)
    }

    private fun viewItem(item: EvmNetworkService.Item): ViewItem {
        return ViewItem(
                item.network.id,
                item.network.name,
                item.network.syncSource.url.toString(),
                item.selected
        )
    }

    val title: String
        get() = when (service.blockchain) {
            EvmNetworkModule.Blockchain.Ethereum -> "Ethereum"
            EvmNetworkModule.Blockchain.BinanceSmartChain -> "Binance Smart Chain"
        }

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
        val viewItems: List<ViewItem>
    )

    data class ViewItem(
        val id: String,
        val name: String,
        val url: String,
        val selected: Boolean,
    )
}
