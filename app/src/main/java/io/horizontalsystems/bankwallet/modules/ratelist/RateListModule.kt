package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.CurrentDateProvider
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.MarketInfo
import java.math.BigDecimal
import java.util.*

object RateListModule {

    interface IView {
        fun show(item: RateListViewItem)
    }

    interface IRouter

    interface IViewDelegate {
        fun viewDidLoad()
    }

    interface IInteractor {
        val currency: Currency
        val coins: List<Coin>

        fun clear()
        fun getMarketInfo(coinCode: String, currencyCode: String): MarketInfo?
        fun subscribeToMarketInfo(currencyCode: String)
        fun setupXRateManager(coinCodes: List<String>)
    }

    interface IInteractorDelegate {
        fun didUpdateMarketInfo(marketInfos: Map<String, MarketInfo>)
    }

    interface IRateListFactory {
        fun rateListViewItem(coins: List<Coin>, currency: Currency, marketInfos: Map<String, MarketInfo?>): RateListViewItem
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
            val presenter = RateListPresenter(view, interactor, RateListFactory(CurrentDateProvider()))

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

class RateListViewItem(val currentDate: Date, val lastUpdateTimestamp: Long?, val rateViewItems: List<RateViewItem>)
class RateViewItem(val coin: Coin, var rateExpired: Boolean?, var rate: CurrencyValue?, var diff: BigDecimal?)
