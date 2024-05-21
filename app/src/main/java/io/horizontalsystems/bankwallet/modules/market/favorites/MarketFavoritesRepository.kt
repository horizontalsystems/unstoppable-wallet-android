package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.filters.TimePeriod
import kotlinx.coroutines.rx2.await

class MarketFavoritesRepository(
    private val marketKit: MarketKitWrapper,
    private val manager: MarketFavoritesManager
) {
    val dataUpdatedObservable by manager::dataUpdatedAsync

    private suspend fun getFavorites(
        currency: Currency,
        period: TimePeriod
    ): List<MarketItem> {
        val favoriteCoins = manager.getAll()
        if (favoriteCoins.isEmpty()) return listOf()

        val favoriteCoinUids = favoriteCoins.map { it.coinUid }
        return marketKit
            .marketInfosSingle(favoriteCoinUids, currency.code).await()
            .map { marketInfo ->
                MarketItem.createFromCoinMarket(
                    marketInfo = marketInfo,
                    currency = currency,
                    period = period
                )
            }
    }

    fun getSignals(uids: List<String>) = marketKit.getCoinSignalsSingle(uids)

    suspend fun get(period: TimePeriod, currency: Currency): List<MarketItem> {
        return getFavorites(currency, period)
    }

    fun removeFavorite(uid: String) {
        manager.remove(uid)
    }
}
