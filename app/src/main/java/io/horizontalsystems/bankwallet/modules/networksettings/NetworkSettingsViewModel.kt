package io.horizontalsystems.bankwallet.modules.networksettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class NetworkSettingsViewModel(private val service: NetworkSettingsService): ViewModel() {

    private val disposables = CompositeDisposable()

    val viewItemsLiveData = MutableLiveData(listOf<ViewItem>())
    val openEvmNetworkLiveEvent = SingleLiveEvent<Pair<EvmBlockchain, Account>>()

    init {
        service.itemsObservable
            .subscribeIO {
                sync(it)
            }
            .let {
                disposables.add(it)
            }

        sync(service.items)
    }

    private fun sync(items: List<NetworkSettingsService.Item>) {
        val viewItems = items.map { item ->
            ViewItem(
                item.blockchain.icon24,
                item.blockchain.shortName,
                item.syncSource.name,
                item
            )
        }

        viewItemsLiveData.postValue(viewItems)
    }

    data class ViewItem(
        val iconResId: Int,
        val title: String,
        val value: String,
        val item: NetworkSettingsService.Item
    )

    fun onSelect(viewItem: ViewItem) {
        openEvmNetworkLiveEvent.postValue(Pair(viewItem.item.blockchain, service.account))
    }

    override fun onCleared() {
        service.clear()
        disposables.clear()
    }
}
