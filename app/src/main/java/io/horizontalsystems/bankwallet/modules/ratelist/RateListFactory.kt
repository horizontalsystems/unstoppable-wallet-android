package io.horizontalsystems.bankwallet.modules.ratelist

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.horizontalsystems.xrateskit.entities.PriceInfo

class RateListFactory(private val numberFormatter: IAppNumberFormatter) : RateListModule.IRateListFactory {

    override fun portfolioViewItems(coins: List<Coin>, currency: Currency, marketInfos: Map<String, MarketInfo?>): List<ViewItem.CoinViewItem> {
        return coins.mapIndexed { index, coin ->
            val marketInfo = marketInfos[coin.code]
            val rateCurrencyValue = marketInfo?.rate?.let { CurrencyValue(currency, it) }
            val diff = if (marketInfo?.isExpired() == true) null else marketInfo?.diff
            val dimRate = (marketInfo?.rate != null && marketInfo.isExpired())

            ViewItem.CoinViewItem(CoinItem(coin.code, coin.title, rate(rateCurrencyValue), diff, coin, rateDimmed = dimRate), index == coins.size - 1)
        }
    }

    override fun topListViewItems(priceInfoItems: List<PriceInfo>, currency: Currency): List<ViewItem.CoinViewItem> {
        return priceInfoItems.mapIndexed { index, priceInfo ->
            ViewItem.CoinViewItem(CoinItem(priceInfo.coinCode, priceInfo.coinName, rate(CurrencyValue(currency, priceInfo.rate)), priceInfo.diff, rateDimmed = false), index == priceInfoItems.size - 1)
        }
    }

    override fun getViewItems(portfolioItems: List<ViewItem.CoinViewItem>, topListItems: List<ViewItem.CoinViewItem>, loading: Boolean): List<ViewItem> {
        val viewItems = mutableListOf<ViewItem>()

        if (portfolioItems.isNotEmpty()) {
            viewItems.add(ViewItem.PortfolioHeader)
            viewItems.addAll(portfolioItems)
        }

        if (topListItems.isEmpty() && loading){
            viewItems.add(ViewItem.LoadingSpinner)
        }
        if (topListItems.isNotEmpty()){
            viewItems.add(ViewItem.TopListHeader)
            viewItems.addAll(topListItems)
            viewItems.add(ViewItem.SourceText)
        }

        return viewItems
    }

    private fun rate(currencyValue: CurrencyValue?): String? {
        currencyValue ?: return null
        return numberFormatter.formatForRates(currencyValue, trimmable = true)
    }

}
