package io.horizontalsystems.bankwallet.modules.market.defi

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.market.top.IMarketListDataSource
import io.horizontalsystems.bankwallet.modules.market.top.MarketTopItem
import io.horizontalsystems.bankwallet.modules.market.top.Period
import io.reactivex.Observable
import io.reactivex.Single

class MarketListDefiDataSource(private val xRateManager: IRateManager) : IMarketListDataSource() {

    override val dataUpdatedAsync: Observable<Unit> = Observable.empty()

    override fun getListAsync(currencyCode: String, period: Period): Single<List<MarketTopItem>> {
        return xRateManager.getTopDefiMarketList(currencyCode, convertPeriod(period))
                .map {
                    it.mapIndexed { index, topMarket ->
                        convertToMarketTopItem(index + 1, topMarket)
                    }
                }
    }

}
