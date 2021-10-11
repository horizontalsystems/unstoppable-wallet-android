package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable

class MarketSearchViewModel(
    private val service: MarketSearchService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItems by lazy {
        val coinCategories = service.coinCategories
        val items = coinCategories.map { category ->
            MarketSearchModule.ViewItem.MarketCoinCategory(category)
        }
        val topCoins = MarketSearchModule.ViewItem.MarketTopCoins
        listOf(topCoins) + items
    }

    private val disposable = CompositeDisposable()

    private val _coinResult = MutableLiveData<List<FullCoin>>()
    val coinResult: LiveData<List<FullCoin>> = _coinResult

    fun searchByQuery(query: String) {
        val queryTrimmed = query.trim()
        if (queryTrimmed.count() >= 2) {
            _coinResult.value = service.getCoinsByQuery(queryTrimmed)
        } else {
            _coinResult.value = listOf()
        }
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }
}
