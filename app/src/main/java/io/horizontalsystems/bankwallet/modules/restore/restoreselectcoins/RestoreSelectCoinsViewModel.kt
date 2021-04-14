package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewItem
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinViewState
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.views.ListPosition
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable

class RestoreSelectCoinsViewModel(
        private val service: RestoreSelectCoinsService,
        private val clearables: List<Clearable>)
    : ViewModel() {

    val viewStateLiveData = MutableLiveData<CoinViewState>()
    val disableCoinLiveData = MutableLiveData<Coin>()
    val successLiveEvent = SingleLiveEvent<Unit>()
    val restoreEnabledLiveData: LiveData<Boolean>
        get() = LiveDataReactiveStreams.fromPublisher(service.canRestore.toFlowable(BackpressureStrategy.DROP))

    private var disposables = CompositeDisposable()

    init {
        service.stateObservable
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

    private fun viewItem(item: RestoreSelectCoinsService.Item, listPosition: ListPosition): CoinViewItem {
        return CoinViewItem(item.coin, item.hasSettings, item.enabled, listPosition)
    }

    private fun syncViewState(serviceState: RestoreSelectCoinsService.State? = null) {
        val state = serviceState ?: service.state

        val viewState = CoinViewState(
                state.featured.mapIndexed { index, item ->
                    viewItem(item, ListPosition.getListPosition(state.featured.size, index))
                },
                state.items.mapIndexed { index, item ->
                    viewItem(item, ListPosition.getListPosition(state.featured.size, index))
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

    fun onRestore() {
        service.restore()
        successLiveEvent.postValue(Unit)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }
}
