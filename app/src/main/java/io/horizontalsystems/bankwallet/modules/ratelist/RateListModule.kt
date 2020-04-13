package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.horizontalsystems.xrateskit.entities.PriceInfo
import java.math.BigDecimal
import java.util.*

object RateListModule {

    interface IView {
        fun showPortfolioItems(items: List<ViewItem>)
        fun showTopListItems(viewItems: List<ViewItem>)
        fun setDates(date: Date, lastUpdateTime: Long?)
    }

    interface IRouter

    interface IViewDelegate {
        fun viewDidLoad()
        fun loadTopList(shownSize: Int)
    }

    interface IInteractor {
        val currency: Currency
        val coins: List<Coin>

        fun clear()
        fun getMarketInfo(coinCode: String, currencyCode: String): MarketInfo?
        fun subscribeToMarketInfo(currencyCode: String)
        fun setupXRateManager(coinCodes: List<String>)
        fun getTopList()
    }

    interface IInteractorDelegate {
        fun didUpdateMarketInfo(marketInfos: Map<String, MarketInfo>)
        fun didFetchedTopList(items: List<PriceInfo>)
    }

    interface IRateListFactory {
        fun portfolioViewItems(coins: List<Coin>, currency: Currency, marketInfos: Map<String, MarketInfo?>): List<ViewItem>
        fun topListViewItems(priceInfoItems: List<PriceInfo>, currency: Currency): List<ViewItem>
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = RateListView()
            val interactor = RatesInteractor(
                    App.xRateManager,
                    App.currencyManager,
                    App.walletStorage,
                    App.appConfigProvider,
                    RateListSorter())
            val presenter = RateListPresenter(view, interactor, RateListFactory(App.numberFormatter))

            interactor.delegate = presenter

            return presenter as T
        }
    }

}

class RateListSorter {
    fun smartSort(coins: List<Coin>, featuredCoins: List<Coin>): List<Coin> {
        return if (coins.isEmpty()) {
            featuredCoins
        } else {
            val filteredByPredefined = featuredCoins.filter { coins.contains(it) }
            val remainingCoins = coins.filter { !featuredCoins.contains(it) }.sortedBy { it.code }
            val mergedList = mutableListOf<Coin>()
            mergedList.addAll(filteredByPredefined)
            mergedList.addAll(remainingCoins)
            mergedList
        }
    }
}

class ViewItem(val coinCode: String, val coinName: String, var rate: String?, var diff: BigDecimal?, val coin: Coin? = null, var rateDimmed: Boolean)
