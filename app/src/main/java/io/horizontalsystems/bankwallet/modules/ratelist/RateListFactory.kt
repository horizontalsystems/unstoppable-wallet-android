package io.horizontalsystems.bankwallet.modules.ratelist

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.horizontalsystems.xrateskit.entities.TopMarket

class RateListFactory(private val numberFormatter: IAppNumberFormatter) : RateListModule.IRateListFactory {

    override fun portfolioViewItems(coins: List<Coin>, currency: Currency, marketInfos: Map<String, MarketInfo>): List<CoinViewItem> {
        return coins.mapIndexed { index, coin ->
            val marketInfo = marketInfos[coin.code]
            val rateCurrencyValue = marketInfo?.rate?.let { CurrencyValue(currency, it) }
            val diff = if (marketInfo?.isExpired() == true) null else marketInfo?.diff
            val dimRate = (marketInfo?.rate != null && marketInfo.isExpired())
            val timestamp = marketInfo?.timestamp ?: 0L

            CoinViewItem(CoinItem(coin.code, coin.title, rate(rateCurrencyValue), diff, coin, timestamp, rateDimmed = dimRate), index == coins.size - 1)
        }
    }

    override fun topListViewItems(topMarketList: List<TopMarket>, currency: Currency): List<CoinViewItem> {
        return topMarketList.mapIndexed { index, topMarket ->
            CoinViewItem(
                    CoinItem(
                            topMarket.coinCode,
                            topMarket.coinName,
                            rate(CurrencyValue(currency, topMarket.marketInfo.rate)),
                            topMarket.marketInfo.diff,
                            timestamp = topMarket.marketInfo.timestamp,
                            rateDimmed = topMarket.marketInfo.isExpired()
                    ), index == topMarketList.size - 1)
        }
    }

    private fun rate(currencyValue: CurrencyValue?): String? {
        currencyValue ?: return null
        return numberFormatter.formatFiat(currencyValue.value, currencyValue.currency.symbol, 0, 2)
    }

}
