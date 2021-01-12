package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.modules.market.top.Field
import io.horizontalsystems.bankwallet.modules.market.top.IMarketListDataSource
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.horizontalsystems.xrateskit.entities.CoinMarket
import io.reactivex.Observable
import io.reactivex.Single

class MarketListFavoritesDataSource(
        private val xRateManager: IRateManager,
        private val marketFavoritesManager: MarketFavoritesManager
) : IMarketListDataSource() {

    override val sortingFields: Array<Field> = Field.values()
    override val dataUpdatedAsync: Observable<Unit> by marketFavoritesManager::dataUpdatedAsync

    override fun doGetListAsync(currencyCode: String, fetchDiffPeriod: TimePeriod): Single<List<CoinMarket>> {
        return Single.zip(
                xRateManager.getTopDefiMarketList(currencyCode, fetchDiffPeriod),
                xRateManager.getTopMarketList(currencyCode, fetchDiffPeriod),
                { t1, t2 ->
                    t1 + t2
                })
                .map { list ->
                    marketFavoritesManager.getAll().mapNotNull { favoriteCoin ->
                        list.find { it.coin.code == favoriteCoin.code }
                    }
                }
    }

}
