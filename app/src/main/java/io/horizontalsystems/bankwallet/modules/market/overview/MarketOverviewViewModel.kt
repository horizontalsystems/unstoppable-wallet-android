package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.modules.market.top.*
import io.reactivex.disposables.CompositeDisposable

class MarketOverviewViewModel(
        private val service: MarketTopService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val topGainersViewItemsLiveData = MutableLiveData<List<MarketTopViewItem>>()
    val topLosersViewItemsLiveData = MutableLiveData<List<MarketTopViewItem>>()
    val topByVolumeViewItemsLiveData = MutableLiveData<List<MarketTopViewItem>>()

    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
                .subscribe {
                    syncState(it)
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun syncState(state: MarketTopService.State) {
        loadingLiveData.postValue(state is MarketTopService.State.Loading)
        errorLiveData.postValue((state as? MarketTopService.State.Error)?.error?.let { convertErrorMessage(it) })

        if (state is MarketTopService.State.Loaded) {
            syncViewItemsBySortingField()
        }
    }

    private fun syncViewItemsBySortingField() {
        topGainersViewItemsLiveData.postValue(sort(service.marketTopItems, Field.TopGainers).subList(0, 3).map(this::convertItemToViewItem))
        topLosersViewItemsLiveData.postValue(sort(service.marketTopItems, Field.TopLosers).subList(0, 3).map(this::convertItemToViewItem))
        topByVolumeViewItemsLiveData.postValue(sort(service.marketTopItems, Field.HighestVolume).subList(0, 3).map(this::convertItemToViewItem))
    }

    private fun convertItemToViewItem(it: MarketTopItem): MarketTopViewItem {
        val formattedRate = App.numberFormatter.formatFiat(it.rate, service.currency.symbol, 2, 2)

        return MarketTopViewItem(it.rank, it.coinCode, it.coinName, formattedRate, it.diff)
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }


    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
        super.onCleared()
    }

    fun refresh() {
        service.refresh()
    }

    private fun sort(items: List<MarketTopItem>, sortingField: Field) = when (sortingField) {
        Field.HighestCap -> items.sortedByDescendingNullLast { it.marketCap }
        Field.LowestCap -> items.sortedByNullLast { it.marketCap }
        Field.HighestVolume -> items.sortedByDescendingNullLast { it.volume }
        Field.LowestVolume -> items.sortedByNullLast { it.volume }
        Field.HighestPrice -> items.sortedByDescendingNullLast { it.rate }
        Field.LowestPrice -> items.sortedByNullLast { it.rate }
        Field.TopGainers -> items.sortedByDescendingNullLast { it.diff }
        Field.TopLosers -> items.sortedByNullLast { it.diff }
    }

}
