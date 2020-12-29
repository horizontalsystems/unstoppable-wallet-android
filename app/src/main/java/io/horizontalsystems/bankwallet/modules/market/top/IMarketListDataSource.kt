package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.storage.FavoriteCoin
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.horizontalsystems.xrateskit.entities.TopMarket
import io.reactivex.Observable
import io.reactivex.Single

abstract class IMarketListDataSource {
    abstract val dataUpdatedAsync: Observable<Unit>
    abstract fun getListAsync(currencyCode: String, period: Period): Single<List<MarketTopItem>>

    protected fun convertToMarketTopItem(rank: Int, topMarket: TopMarket) =
            MarketTopItem(
                    rank,
                    topMarket.coin.code,
                    topMarket.coin.title,
                    topMarket.marketInfo.marketCap.toDouble(),
                    topMarket.marketInfo.volume.toDouble(),
                    topMarket.marketInfo.rate,
                    topMarket.marketInfo.rateDiffPeriod,
            )

    protected fun convertPeriod(period: Period) = when (period) {
        Period.Period24h -> TimePeriod.HOUR_24
        Period.PeriodWeek -> TimePeriod.DAY_7
        Period.PeriodMonth -> TimePeriod.DAY_30
    }

}

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

class MarketListFavoritesDataSource(
        private val xRateManager: IRateManager,
        private val marketFavoritesManager: MarketFavoritesManager
) : IMarketListDataSource() {

    override val dataUpdatedAsync: Observable<Unit> by marketFavoritesManager::dataUpdatedAsync
    private var cachedTopMarketList: List<TopMarket>? = null

    override fun getListAsync(currencyCode: String, period: Period): Single<List<MarketTopItem>> {
        return getTopMarketList(currencyCode)
                .map {
                    it.filter { isCoinInFavorites(it, marketFavoritesManager.getAll()) }
                            .mapIndexed { index, topMarket ->
                                convertToMarketTopItem(index + 1, topMarket)
                            }
                }
    }

    private fun getTopMarketList(currencyCode: String) = when {
        cachedTopMarketList != null -> {
            Single.just(cachedTopMarketList)
        }
        else -> {
            xRateManager.getTopMarketList(currencyCode, TimePeriod.HOUR_24)
                    .doOnSuccess {
                        cachedTopMarketList = it
                    }
        }
    }

    private fun isCoinInFavorites(topMarket: TopMarket, favoriteCoins: List<FavoriteCoin>): Boolean {
        return favoriteCoins.find { it.code == topMarket.coin.code } != null
    }

}
