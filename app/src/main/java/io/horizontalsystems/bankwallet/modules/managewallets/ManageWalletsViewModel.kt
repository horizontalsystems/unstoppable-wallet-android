package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewState
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsViewModel(
        private val service: ManageWalletsService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val viewStateLiveData = MutableLiveData<CoinViewState>()
    val disableCoinLiveData = MutableLiveData<Coin>()

    private var disposables = CompositeDisposable()

    init {
        service.stateAsync
                .subscribeIO {
                    syncViewState(it)
                }
                .let {
                    disposables.add(it)
                }

        service.cancelEnableCoinAsync
                .subscribeIO {
                    disableCoinLiveData.postValue(it)
                }.let {
                    disposables.add(it)
                }

        syncViewState()
    }

    private fun viewItem(item: ManageWalletsService.Item, listPosition: ListPosition): CoinViewItem {
        return CoinViewItem(item.coin, item.hasSettings, item.enabled, listPosition)
    }

    private fun syncViewState(serviceState: ManageWalletsService.State? = null) {
        val state = serviceState ?: service.state

        val viewState = CoinViewState(
                state.featuredItems.mapIndexed { index, item ->
                    viewItem(item, ListPosition.getListPosition(state.featuredItems.size, index))
                },
                state.items.mapIndexed { index, item ->
                    viewItem(item, ListPosition.getListPosition(state.items.size, index))
                },
        )

        viewStateLiveData.postValue(viewState)
    }

    fun enable(coin: Coin) {
        service.enable(coin)
    }

    fun disable(coin: Coin) {
        service.disable(coin)
    }

    fun onClickSettings(coin: Coin) {
        service.configure(coin)
    }

    fun updateFilter(v: String) {
        service.setFilter(v)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

}
