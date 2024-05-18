package cash.p.terminal.modules.market.favorites

import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.market.MarketItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.favorites.MarketFavoritesModule.Period
import cash.p.terminal.modules.market.sort
import kotlinx.coroutines.rx2.await

class MarketFavoritesRepository(
    private val marketKit: MarketKitWrapper,
    private val manager: MarketFavoritesManager
) {
    val dataUpdatedObservable by manager::dataUpdatedAsync

    private suspend fun getFavorites(
        currency: Currency,
        period: Period
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

    suspend fun get(
        sortDescending: Boolean,
        period: Period,
        currency: Currency,
    ): List<MarketItem> {
        val sortingField = if (sortDescending) SortingField.TopGainers else SortingField.TopLosers
        val marketItems = getFavorites(currency, period)
        return marketItems.sort(sortingField)
    }

    fun removeFavorite(uid: String) {
        manager.remove(uid)
    }
}
