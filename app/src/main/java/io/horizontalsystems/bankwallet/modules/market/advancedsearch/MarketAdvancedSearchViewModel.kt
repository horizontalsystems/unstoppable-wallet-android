package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper

class MarketAdvancedSearchViewModel(private val service: MarketAdvancedSearchService) : ViewModel() {

    val coinLists = CoinList.values().map {
        ViewItemWrapper(App.instance.getString(it.titleResId), it)
    }

    var coinList
        get() = ViewItemWrapper(App.instance.getString(service.coinList.titleResId), service.coinList)
        set(value) {
            service.coinList = value.item
        }

    val marketCaps = listOf(ViewItemWrapper<MarketCap?>(App.instance.getString(R.string.None), null)) +
            MarketCap.values().map {
                ViewItemWrapper<MarketCap?>(App.instance.getString(it.titleResId), it)
            }

    var marketCap = ViewItemWrapper<MarketCap?>(App.instance.getString(R.string.None), null)

    fun showResults() = Unit

}
