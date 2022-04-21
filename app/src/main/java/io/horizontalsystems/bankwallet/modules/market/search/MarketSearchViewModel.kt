package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class MarketSearchViewModel(
    private val service: MarketSearchService
) : ViewModel() {

    private val disposables = CompositeDisposable()

    val timePeriodMenu by service::timePeriodMenu
    val sortDescending by service::sortDescending
    val screenState by service::screenState

    init {
        viewModelScope.launch {
            service.start()
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

    fun toggleTimePeriod(timeDuration: TimeDuration) {
        service.setTimePeriod(timeDuration)
    }

    fun toggleSortType() {
        service.toggleSortType()
    }

}
