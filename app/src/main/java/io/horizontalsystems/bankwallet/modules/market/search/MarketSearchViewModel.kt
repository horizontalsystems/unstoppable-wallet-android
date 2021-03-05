package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO

class MarketSearchViewModel(private val service: MarketSearchService, private val clearables: List<Clearable>) : ViewModel() {

    var query: String by service::query

    val itemsLiveData = MutableLiveData<List<CoinDataViewItem>>()
    val emptyResultsLiveData = MutableLiveData(false)
    val advancedSearchButtonVisibleLiveDataViewItem = MutableLiveData(service.query.isBlank())

    init {
        service.itemsAsync
                .subscribeIO {
                    emptyResultsLiveData.postValue(it.isPresent && it.get().isEmpty())
                    advancedSearchButtonVisibleLiveDataViewItem.postValue(service.query.isBlank())

                    itemsLiveData.postValue(it.orElse(listOf()).map { CoinDataViewItem(it.code, it.title, it.type) })
                }
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

}
