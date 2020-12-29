package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.IRateManager
import io.reactivex.Observable
import io.reactivex.Single

class MarketListTopDataSource(private val xRateManager: IRateManager) : IMarketListDataSource() {

    override val dataUpdatedAsync: Observable<Unit> = Observable.empty()

    override fun getListAsync(currencyCode: String, period: Period): Single<List<MarketTopItem>> {
        return xRateManager.getTopMarketList(currencyCode, convertPeriod(period))
                .map {
                    it.mapIndexed { index, topMarket ->
                        convertToMarketTopItem(index + 1, topMarket)
                    }
                }
    }

}
