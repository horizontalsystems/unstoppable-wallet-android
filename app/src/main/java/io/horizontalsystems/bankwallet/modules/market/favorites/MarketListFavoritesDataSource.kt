package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.modules.market.top.Field
import io.horizontalsystems.bankwallet.modules.market.top.IMarketListDataSource
import io.horizontalsystems.xrateskit.entities.Coin
import io.horizontalsystems.xrateskit.entities.CoinMarket
import io.reactivex.Observable
import io.reactivex.Single

class MarketListFavoritesDataSource(
        private val xRateManager: IRateManager,
        private val marketFavoritesManager: MarketFavoritesManager
) : IMarketListDataSource {

    override val sortingFields: Array<Field> = Field.values()
    override val dataUpdatedAsync: Observable<Unit> by marketFavoritesManager::dataUpdatedAsync

    override fun getListAsync(currencyCode: String): Single<List<CoinMarket>> {
        val coinCodes = marketFavoritesManager.getAll().map { it.code }

        return xRateManager.getCoinMarketList(coinCodes, currencyCode)
    }

}
