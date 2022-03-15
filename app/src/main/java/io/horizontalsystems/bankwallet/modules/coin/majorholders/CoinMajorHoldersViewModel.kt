package io.horizontalsystems.bankwallet.modules.coin.majorholders

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinViewFactory
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.reactivex.disposables.CompositeDisposable

class CoinMajorHoldersViewModel(
    private val service: CoinMajorHoldersService,
    private val factory: CoinViewFactory
) : ViewModel() {

    private val disposables = CompositeDisposable()

    val viewStateLiveData = MutableLiveData<ViewState?>(null)
    val coinMajorHolders = MutableLiveData<List<MajorHolderItem>>()
    val isRefreshingLiveData = MutableLiveData<Boolean>()

    init {
        service.stateObservable
            .subscribeIO({ state ->
                isRefreshingLiveData.postValue(false)

                when (state) {
                    is DataState.Success -> {
                        viewStateLiveData.postValue(ViewState.Success)

                        coinMajorHolders.postValue(factory.getCoinMajorHolders(state.data))
                    }
                    is DataState.Error -> {
                        viewStateLiveData.postValue(ViewState.Error(state.error))
                    }
                }
            }, {

            }).let { disposables.add(it) }

        service.start()
    }

    fun onErrorClick() {
        service.refresh()
    }

    fun refresh() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }
}
