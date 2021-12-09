package io.horizontalsystems.bankwallet.modules.evmnetwork

import android.util.Log
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

    val sectionViewItemsLiveData = MutableLiveData<List<SectionViewItem>>()
    val finishLiveEvent = SingleLiveEvent<Unit>()
    val confirmLiveEvent = SingleLiveEvent<Unit>()

    private var tmpViewItem: ViewItem? = null

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
            sectionViewItem("MainNet", mainNetItems),
            sectionViewItem("TestNet", testNetItems)
        )

        sectionViewItemsLiveData.postValue(sectionViewItems)
    }

    private fun sectionViewItem(title: String, items: List<EvmNetworkService.Item>): SectionViewItem? {
        val viewItems = items.map { viewItem(it) }.sortedBy { it.name }
        val selectedItem = items.firstOrNull { it.selected }

        val description = selectedItem?.let {
            val urls = selectedItem.network.syncSource.urls
            val formattedUrls = if (urls.size > 1) urls.joinToString(separator = "") { "  •  $it \n" } else null
            formattedUrls?.let { "${Translator.getString(R.string.NetworkSettings_SwithesAutomatically_Description)}\n\n$formattedUrls" }
        }

        Log.e("AAA", "description: \n$description")

        if (viewItems.isEmpty()) return null

        return SectionViewItem(title, viewItems, description)
    }

    private fun viewItem(item: EvmNetworkService.Item): ViewItem {
        val url = if (item.network.syncSource.urls.size == 1)
            item.network.syncSource.urls.first().toString()
        else
            Translator.getString(R.string.NetworkSettings_SwithesAutomatically)

        return ViewItem(
            item.network.id,
            item.network.name,
            url,
            item.selected
        )
    }

    val title: String
        get() = when (service.blockchain) {
            EvmNetworkModule.Blockchain.Ethereum -> "Ethereum"
            EvmNetworkModule.Blockchain.BinanceSmartChain -> "Binance Smart Chain"
        }

    fun onSelectViewItem(viewItem: ViewItem) {
        if (service.isConfirmationRequired(viewItem.id)) {
            tmpViewItem = viewItem
            confirmLiveEvent.postValue(Unit)
        } else {
            setNetwork(viewItem)
        }
    }

    fun confirmSelection() {
        tmpViewItem?.let {
            setNetwork(it)
            tmpViewItem = null
        }
    }

    private fun setNetwork(viewItem: ViewItem) {
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
