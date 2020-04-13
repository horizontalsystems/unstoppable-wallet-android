package io.horizontalsystems.bankwallet.modules.ratelist

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.horizontalsystems.xrateskit.entities.PriceInfo

class RateListFactory(private val numberFormatter: IAppNumberFormatter) : RateListModule.IRateListFactory {

    override fun portfolioViewItems(coins: List<Coin>, currency: Currency, marketInfos: Map<String, MarketInfo?>): List<ViewItem> {
        return coins.map { coin ->
            val marketInfo = marketInfos[coin.code]
            val rateCurrencyValue = marketInfo?.rate?.let { CurrencyValue(currency, it) }
            val diff = if (marketInfo?.isExpired() == true) null else marketInfo?.diff
            val dimRate = (marketInfo?.rate != null && marketInfo.isExpired())

            ViewItem(coin.code, coin.title, rate(rateCurrencyValue), diff, coin, rateDimmed = dimRate)
        }
    }

    override fun topListViewItems(priceInfoItems: List<PriceInfo>, currency: Currency): List<ViewItem> {
        return priceInfoItems.map {
            ViewItem(it.coinCode, it.coinName, rate(CurrencyValue(currency, it.rate)), it.diff, rateDimmed = false)
        }
    }

    private fun rate(currencyValue: CurrencyValue?): String? {
        currencyValue ?: return null
        return numberFormatter.formatForRates(currencyValue, trimmable = true)
    }

}
