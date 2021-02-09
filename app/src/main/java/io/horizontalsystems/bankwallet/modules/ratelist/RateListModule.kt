package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal

object RateListModule {

    interface IView {
        fun setDate(lastUpdateTime: Long)
        fun setPortfolioViewItems(viewItems: List<CoinItem>)
        fun setTopViewItems(viewItems: List<CoinItem>)
    }

    interface IRouter {
        fun openChart(coinCode: String, coinTitle: String)
        fun openSortingTypeDialog(sortType: TopListSortType)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onCoinClicked(coinItem: CoinItem)
        fun onTopListSortClick()
        fun onTopListSortTypeChange(sortType: TopListSortType)
    }

    interface IInteractor {
        val currency: Currency
        val coins: List<Coin>

        fun clear()
        fun getMarketInfo(coinCode: String, currencyCode: String): MarketInfo?
        fun subscribeToMarketInfo(currencyCode: String)
        fun setupXRateManager(coins: List<Coin>)
        fun getTopList()
    }

    interface IInteractorDelegate {
        fun didUpdateMarketInfo(marketInfos: Map<String, MarketInfo>)
        fun didFetchedTopMarketList(items: List<TopMarketRanked>)
        fun didFailToFetchTopList()
    }

    interface IRateListFactory {
        fun portfolioViewItems(coins: List<Coin>, currency: Currency, marketInfos: Map<String, MarketInfo>): List<CoinItem>
        fun topListViewItems(topMarketList: List<TopMarketRanked>, currency: Currency): List<CoinItem>
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = RateListView()
            val router = RateListRouter()
            val interactor = RatesInteractor(
                    App.xRateManager,
                    App.currencyManager,
                    App.walletStorage,
                    App.appConfigProvider,
                    RateListSorter())
            val presenter = RateListPresenter(view, router, interactor, RateListFactory(App.numberFormatter))

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

data class CoinItem(val coinCode: String, val coinName: String, var rate: String?, var diff: BigDecimal?, val coin: Coin? = null, var timestamp: Long, var rateDimmed: Boolean, val rank: Int? = null)

data class TopMarketRanked(
        val coinCode: String,
        val coinName: String,
        val marketInfo: MarketInfo,
        val rank: Int
)