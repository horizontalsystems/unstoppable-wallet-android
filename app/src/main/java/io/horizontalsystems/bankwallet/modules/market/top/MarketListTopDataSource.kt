package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.xrateskit.entities.CoinMarket
import io.reactivex.Observable
import io.reactivex.Single

class MarketListTopDataSource(private val xRateManager: IRateManager) : IMarketListDataSource {

    override val sortingFields: Array<SortingField> = arrayOf(
            SortingField.HighestCap,
            SortingField.LowestCap,
            SortingField.HighestVolume,
            SortingField.LowestVolume,
            SortingField.HighestPrice,
            SortingField.LowestPrice,
            SortingField.TopGainers,
            SortingField.TopLosers,
    )

    override val dataUpdatedAsync: Observable<Unit> = Observable.empty()

    override fun getListAsync(currencyCode: String): Single<List<CoinMarket>> {
        return xRateManager.getTopMarketList(currencyCode)
    }
}
