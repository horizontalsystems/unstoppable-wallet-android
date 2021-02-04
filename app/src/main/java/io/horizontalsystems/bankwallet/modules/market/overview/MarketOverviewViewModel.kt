package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import io.reactivex.disposables.CompositeDisposable

class MarketOverviewViewModel(
        private val service: MarketOverviewService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val topGainersViewItemsLiveData = MutableLiveData<List<MarketViewItem>>()
    val topLosersViewItemsLiveData = MutableLiveData<List<MarketViewItem>>()
    val topByVolumeViewItemsLiveData = MutableLiveData<List<MarketViewItem>>()
    val showPoweredByLiveData = MutableLiveData(false)

    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)
    val toastLiveData = MutableLiveData<String>()

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
                .subscribeIO {
                    syncState(it)
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun syncState(state: MarketOverviewService.State) {
        if (service.marketItems.isNotEmpty() && state is MarketOverviewService.State.Error) {
            toastLiveData.postValue(convertErrorMessage(state.error))
        } else {
            loadingLiveData.postValue(state is MarketOverviewService.State.Loading)
            errorLiveData.postValue((state as? MarketOverviewService.State.Error)?.let { convertErrorMessage(it.error) })

            if (state is MarketOverviewService.State.Loaded) {
                syncViewItemsBySortingField()
            }
        }

        showPoweredByLiveData.postValue(service.marketItems.isNotEmpty())
    }

    private fun syncViewItemsBySortingField() {
        topGainersViewItemsLiveData.postValue(service.marketItems.sort(SortingField.TopGainers).subList(0, 3).map { MarketViewItem.create(it, this.service.currency.symbol, MarketField.PriceDiff) })
        topLosersViewItemsLiveData.postValue(service.marketItems.sort(SortingField.TopLosers).subList(0, 3).map { MarketViewItem.create(it, this.service.currency.symbol, MarketField.PriceDiff) })
        topByVolumeViewItemsLiveData.postValue(service.marketItems.sort(SortingField.HighestVolume).subList(0, 3).map { MarketViewItem.create(it, this.service.currency.symbol, MarketField.Volume) })
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }

    fun onErrorClick() {
        service.refresh()
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
    }

    fun refresh() {
        service.refresh()
    }
}
