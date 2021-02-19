package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper

class MarketAdvancedSearchViewModel(private val service: MarketAdvancedSearchService) : ViewModel() {

    val coinLists = CoinList.values().map {
        ViewItemWrapper(App.instance.getString(it.titleResId), it, R.color.leah)
    }

    var coinList
        get() = ViewItemWrapper(App.instance.getString(service.coinList.titleResId), service.coinList, R.color.leah)
        set(value) {
            service.coinList = value.item
        }

    val marketCaps = ranges
    var marketCap = rangeEmpty

    val volumes = ranges
    var volume = rangeEmpty

    val liquidities = ranges
    var liquidity = rangeEmpty

    val periods = listOf(ViewItemWrapper<TimePeriod?>(App.instance.getString(R.string.None), null, R.color.grey)) + TimePeriod.values().map {
        ViewItemWrapper<TimePeriod?>(App.instance.getString(it.titleResId), it, R.color.leah)
    }
    var period = ViewItemWrapper<TimePeriod?>(App.instance.getString(R.string.None), null, R.color.grey)

    fun showResults() = Unit
}

val rangeEmpty = ViewItemWrapper<Range?>(App.instance.getString(R.string.None), null, R.color.grey)
val ranges = listOf(rangeEmpty) + Range.values().map {
    ViewItemWrapper<Range?>(App.instance.getString(it.titleResId), it, R.color.leah)
}
