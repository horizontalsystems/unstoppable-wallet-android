package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.disposables.CompositeDisposable

class MarketSearchViewModel(
    private val service: MarketSearchService
) : ViewModel() {

    private val disposables = CompositeDisposable()

    val screenStateLiveData = MutableLiveData<MarketSearchModule.DataState>()

    init {
        service.stateObservable
            .subscribeIO {
                screenStateLiveData.postValue(it)
            }.let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    fun searchByQuery(query: String) {
        service.setFilter(query.trim())
    }

    fun onFavoriteClick(favourited: Boolean, coinUid: String) {
        if (favourited) {
            service.unFavorite(coinUid)
        } else {
            service.favorite(coinUid)
        }
    }

}
