package cash.p.terminal.modules.market.favorites

import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.market.MarketItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.favorites.MarketFavoritesModule.Period
import cash.p.terminal.modules.market.sort
import io.reactivex.Single

class MarketFavoritesRepository(
    private val marketKit: MarketKitWrapper,
    private val manager: MarketFavoritesManager
) {
    val dataUpdatedObservable by manager::dataUpdatedAsync

    private fun getFavorites(
        currency: Currency,
        period: Period
    ): List<MarketItem> {
        val favoriteCoins = manager.getAll()
        var marketItems = listOf<MarketItem>()
        if (favoriteCoins.isNotEmpty()) {
            val favoriteCoinUids = favoriteCoins.map { it.coinUid }
            marketItems = marketKit.marketInfosSingle(favoriteCoinUids, currency.code).blockingGet()
                .map { marketInfo ->
                    MarketItem.createFromCoinMarket(
                        marketInfo = marketInfo,
                        currency = currency,
                        period = period
                    )
                }
        }
        return marketItems
    }

    fun get(
        sortDescending: Boolean,
        period: Period,
        currency: Currency,
    ): Single<List<MarketItem>> =
        Single.create { emitter ->
            val sortingField = if (sortDescending) SortingField.TopGainers else SortingField.TopLosers
            try {
                val marketItems = getFavorites(currency, period)
                emitter.onSuccess(
                    marketItems.sort(sortingField)
                )
            } catch (error: Throwable) {
                emitter.onError(error)
            }
        }

    fun removeFavorite(uid: String) {
        manager.remove(uid)
    }
}
