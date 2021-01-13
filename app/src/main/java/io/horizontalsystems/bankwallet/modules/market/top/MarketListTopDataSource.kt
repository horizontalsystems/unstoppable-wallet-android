package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.horizontalsystems.xrateskit.entities.CoinMarket
import io.reactivex.Observable
import io.reactivex.Single

class MarketListTopDataSource(private val xRateManager: IRateManager) : IMarketListDataSource {

    override val sortingFields: Array<Field> = arrayOf(
            Field.HighestCap,
            Field.LowestCap,
            Field.HighestVolume,
            Field.LowestVolume,
            Field.HighestPrice,
            Field.LowestPrice,
            Field.TopGainers,
            Field.TopLosers,
    )

    override val dataUpdatedAsync: Observable<Unit> = Observable.empty()

    override fun getListAsync(currencyCode: String, fetchDiffPeriod: TimePeriod): Single<List<CoinMarket>> {
        return xRateManager.getTopMarketList(currencyCode, fetchDiffPeriod)
    }
}
