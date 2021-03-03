package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewState
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class RestoreSelectCoinsViewModel(
        private val service: RestoreSelectCoinsService,
        private val clearables: List<Clearable>)
    : ViewModel() {

    val viewStateLiveData = MutableLiveData<CoinViewState>()
    val enabledCoinsLiveData = SingleLiveEvent<List<Coin>>()
    val canRestoreLiveData = MutableLiveData<Boolean>()

    private var disposables = CompositeDisposable()
    private var filter: String? = null

    init {
        syncViewState()

        service.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncViewState(it)
                }
                .let {
                    disposables.add(it)
                }

        service.canRestore
                .subscribe {
                    canRestoreLiveData.postValue(it)
                }.let { disposables.add(it) }

        service.cancelEnableCoinAsync
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncViewState()
                }.let {
                    disposables.add(it)
                }
    }

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        disposables.clear()
        super.onCleared()
    }

    fun enable(coin: Coin) {
        service.enable(coin)
    }

    fun disable(coin: Coin) {
        service.disable(coin)
    }

    fun updateFilter(newText: String?) {
        filter = newText
        syncViewState()
    }

    fun onRestore() {
        enabledCoinsLiveData.postValue(service.enabledCoins)
    }

    private fun syncViewState(serviceState: RestoreSelectCoinsService.State? = null) {
        val state = serviceState ?: service.state

        val filteredFeatureCoins = filtered(state.featured)

        val filteredItems = filtered(state.items)

        viewStateLiveData.postValue(CoinViewState(
                filteredFeatureCoins.mapIndexed { index, item ->
                    viewItem(item, ListPosition.getListPosition(filteredFeatureCoins.size, index))
                },
                filteredItems.mapIndexed { index, item ->
                    viewItem(item, ListPosition.getListPosition(filteredItems.size, index))
                }
        ))
    }

    private fun viewItem(item: RestoreSelectCoinsService.Item, listPosition: ListPosition): CoinViewItem {
        return CoinViewItem.ToggleVisible(item.coin, item.enabled, listPosition)
    }

    private fun filtered(items: List<RestoreSelectCoinsService.Item>): List<RestoreSelectCoinsService.Item> {
        val filter = filter ?: return items

        return items.filter {
            it.coin.title.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
                    || it.coin.code.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
        }
    }

}
