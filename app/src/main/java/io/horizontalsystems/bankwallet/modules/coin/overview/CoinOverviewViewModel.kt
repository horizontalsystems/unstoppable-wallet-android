package io.horizontalsystems.bankwallet.modules.coin.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinViewFactory
import io.reactivex.disposables.CompositeDisposable

class CoinOverviewViewModel(
    private val service: CoinOverviewService,
    private val factory: CoinViewFactory,
) : ViewModel() {

    val fullCoin by service::fullCoin
    val isRefreshingLiveData = MutableLiveData<Boolean>(false)
    val overviewLiveData = MutableLiveData<CoinOverviewViewItem>()
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)

    private val disposables = CompositeDisposable()

    init {
        service.coinOverviewObservable
            .subscribeIO { coinOverview ->
                isRefreshingLiveData.postValue(false)

                coinOverview.dataOrNull?.let {
                    overviewLiveData.postValue(factory.getOverviewViewItem(it))
                }

                coinOverview.viewState?.let {
                    viewStateLiveData.postValue(it)
                }
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    fun refresh() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }

    fun retry() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }
}
