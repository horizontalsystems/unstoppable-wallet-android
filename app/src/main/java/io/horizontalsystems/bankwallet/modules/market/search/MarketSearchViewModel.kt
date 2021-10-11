package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.ui.helpers.ImageWebHelper
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable

class MarketSearchViewModel(
    private val service: MarketSearchService,
    private val translator: Translator,
    private val clearables: List<Clearable>
) : ViewModel() {

    val viewItems by lazy {
        val coinCategories = service.coinCategories
        val items = coinCategories.map { category ->
            MarketSearchModule.ViewItem(
                name = category.name,
                description = category.description["en"] ?: "",
                imageUrl = ImageWebHelper.getCoinCategoryImageUrl(category.uid),
                type = MarketSearchModule.ViewType.CoinCategoryType(category)
            )
        }
        val topCoins = MarketSearchModule.ViewItem(
            name = translator.getString(R.string.Market_Category_TopCoins),
            description = translator.getString(R.string.Market_Category_TopCoins_Description),
            imageUrl = ImageWebHelper.getCoinCategoryImageUrl("top_coins"),
            type = MarketSearchModule.ViewType.TopCoinsType
        )
        val list = mutableListOf(topCoins)
        list.addAll(items)
        return@lazy list
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
