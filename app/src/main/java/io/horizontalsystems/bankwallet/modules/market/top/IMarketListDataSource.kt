package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.storage.FavoriteCoin
import io.horizontalsystems.xrateskit.entities.TopMarket
import io.reactivex.Observable
import io.reactivex.Single

interface IMarketListDataSource {
    val dataUpdatedAsync: Observable<Unit>
    fun getListAsync(currencyCode: String): Single<List<TopMarket>>
}

class MarketListTopDataSource(private val xRateManager: IRateManager) : IMarketListDataSource {

    override val dataUpdatedAsync: Observable<Unit> = Observable.empty()

    override fun getListAsync(currencyCode: String): Single<List<TopMarket>> {
        return xRateManager.getTopMarketList(currencyCode)
    }

}

class MarketListFavoritesDataSource(
        private val xRateManager: IRateManager,
        private val marketFavoritesManager: MarketFavoritesManager
) : IMarketListDataSource {

    override val dataUpdatedAsync: Observable<Unit> by marketFavoritesManager::dataUpdatedAsync
    private var cachedTopMarketList: List<TopMarket>? = null

    override fun getListAsync(currencyCode: String): Single<List<TopMarket>> {
        return getTopMarketList(currencyCode)
                .map {
                    it.filter { isCoinInFavorites(it, marketFavoritesManager.getAll()) }
                }
    }

    private fun getTopMarketList(currencyCode: String) = when {
        cachedTopMarketList != null -> {
            Single.just(cachedTopMarketList)
        }
        else -> {
            xRateManager.getTopMarketList(currencyCode)
                    .doOnSuccess {
                        cachedTopMarketList = it
                    }
        }
    }

    private fun isCoinInFavorites(topMarket: TopMarket, favoriteCoins: List<FavoriteCoin>): Boolean {
        return favoriteCoins.find { it.code == topMarket.coinCode } != null
    }

}
