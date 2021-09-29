package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItemState
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsViewModel(
    private val service: ManageWalletsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItemsLiveData = MutableLiveData<List<CoinViewItem>>()
    val disableCoinLiveData = MutableLiveData<Coin>()

    private var disposables = CompositeDisposable()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        service.cancelEnableCoinObservable
            .subscribeIO { disableCoinLiveData.postValue(it) }
            .let { disposables.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<ManageWalletsService.Item>) {
        val itemsSize = items.size
        val viewItems = items.mapIndexed { index, item -> viewItem(item, ListPosition.getListPosition(itemsSize, index)) }
        viewItemsLiveData.postValue(viewItems)
    }

    private fun viewItem(item: ManageWalletsService.Item, listPosition: ListPosition): CoinViewItem {
        return when (item.state) {
            is ManageWalletsService.ItemState.Supported -> {
                CoinViewItem(
                    item.fullCoin,
                    CoinViewItemState.ToggleVisible(item.state.enabled, item.state.hasSettings),
                    listPosition
                )
            }
            ManageWalletsService.ItemState.Unsupported -> {
                CoinViewItem(item.fullCoin, CoinViewItemState.ToggleHidden, listPosition)
            }
        }
    }

    fun enable(fullCoin: FullCoin) {
        service.enable(fullCoin)
    }

    fun disable(fullCoin: FullCoin) {
        service.disable(fullCoin)
    }

    fun onClickSettings(fullCoin: FullCoin) {
        service.configure(fullCoin)
    }

    fun updateFilter(filter: String) {
        service.setFilter(filter)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

}
