package io.horizontalsystems.bankwallet.modules.ratelist

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.MarketInfo

class RateListFactory(private val numberFormatter: IAppNumberFormatter) : RateListModule.IRateListFactory {

    override fun portfolioViewItems(coins: List<Coin>, currency: Currency, marketInfos: Map<String, MarketInfo>): List<CoinItem> {
        return coins.map { coin ->
            val marketInfo = marketInfos[coin.code]
            val rateCurrencyValue = marketInfo?.rate?.let { CurrencyValue(currency, it) }
            val diff = if (marketInfo?.isExpired() == true) null else marketInfo?.rateDiff
            val dimRate = (marketInfo?.rate != null && marketInfo.isExpired())
            val timestamp = marketInfo?.timestamp ?: 0L

            CoinItem(coin.code, coin.title, rate(rateCurrencyValue), diff, coin, timestamp, rateDimmed = dimRate)
        }
    }

    override fun topListViewItems(topMarketList: List<TopMarketRanked>, currency: Currency): List<CoinItem> {
        return topMarketList.map { topMarket ->
            CoinItem(
                    topMarket.coinCode,
                    topMarket.coinName,
                    rate(CurrencyValue(currency, topMarket.marketInfo.rate)),
                    topMarket.marketInfo.rateDiff,
                    timestamp = topMarket.marketInfo.timestamp,
                    rateDimmed = topMarket.marketInfo.isExpired(),
                    rank = topMarket.rank
            )
        }
    }

    private fun rate(currencyValue: CurrencyValue?): String? {
        return currencyValue?.let {
            numberFormatter.formatFiat(it.value, it.currency.symbol, 0, 2)
        }
    }

}
