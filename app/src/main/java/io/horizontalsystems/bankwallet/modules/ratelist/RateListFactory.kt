package io.horizontalsystems.bankwallet.modules.ratelist

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.ICurrentDateProvider
import io.horizontalsystems.xrateskit.entities.MarketInfo

class RateListFactory(private val currentDateProvider: ICurrentDateProvider) : RateListModule.IRateListFactory {

    override fun rateListViewItem(coins: List<Coin>, currency: Currency, marketInfos: Map<String, MarketInfo?>): RateListViewItem {
        val items = coins.map { viewItem(it, currency, marketInfos[it.code]) }
        return RateListViewItem(currentDateProvider.currentDate, lastUpdateTimestamp(marketInfos), items)
    }

    private fun lastUpdateTimestamp(marketInfos: Map<String, MarketInfo?>): Long? {
        val allTimestamps = marketInfos.map { it.value?.timestamp }.filterNotNull()
        return allTimestamps.max()
    }

    private fun viewItem(coin: Coin, currency: Currency, marketInfo: MarketInfo?): RateViewItem {
        val rateValue = marketInfo?.rate?.let { CurrencyValue(currency, it) }
        return RateViewItem(coin, marketInfo?.isExpired(), rateValue, marketInfo?.diff)
    }
}
