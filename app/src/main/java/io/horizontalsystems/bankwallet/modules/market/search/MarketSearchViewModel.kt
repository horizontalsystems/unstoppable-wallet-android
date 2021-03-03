package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.coinkit.models.CoinType

class MarketSearchViewModel(private val service: MarketSearchService, private val clearables: List<Clearable>) : ViewModel() {

    var query: String by service::query

    val itemsLiveData = MutableLiveData<List<CoinDataViewItem>>()
    val emptyResultsLiveData = MutableLiveData(false)
    val loadingLiveData = MutableLiveData(false)
    val advancedSearchButtonVisibleLiveDataViewItem = MutableLiveData(service.query.isBlank())

    init {
        service.stateAsync
                .subscribeIO {
                    loadingLiveData.postValue(it is MarketSearchService.State.Loading)
                    emptyResultsLiveData.postValue(it is MarketSearchService.State.Success && it.items.isEmpty())
                    advancedSearchButtonVisibleLiveDataViewItem.postValue(service.query.isBlank())

                    if (it is MarketSearchService.State.Success) {
                        itemsLiveData.postValue(it.items.map { CoinDataViewItem(it.code, it.title, it.type) })
                    } else {
                        itemsLiveData.postValue(listOf())
                    }
                }
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

}
