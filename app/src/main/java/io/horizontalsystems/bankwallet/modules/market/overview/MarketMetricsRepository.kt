package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewModule.MarketMetricsItem
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Single

class MarketMetricsRepository(
    private val xRateManager: IRateManager
) {
    private var marketMetricsItemCache: MarketMetricsItem? = null

    fun get(baseCurrency: Currency, forceRefresh: Boolean): Single<MarketMetricsItem> =
        if (forceRefresh || marketMetricsItemCache == null) {
            xRateManager.getGlobalMarketInfoAsync(baseCurrency.code)
                .map {
                    marketMetricsItemCache = MarketMetricsItem.createFromGlobalCoinMarket(it, baseCurrency)
                    marketMetricsItemCache
                }
        } else {
            Single.just(marketMetricsItemCache)
        }
}
