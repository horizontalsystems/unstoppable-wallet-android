package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import io.reactivex.Single

class MarketFavoritesRepository(
    private val marketKit: MarketKitWrapper,
    private val manager: MarketFavoritesManager
) {
    private var cache: List<MarketItem> = listOf()

    val dataUpdatedObservable by manager::dataUpdatedAsync

    private fun getFavorites(
        forceRefresh: Boolean,
        currency: Currency
    ): List<MarketItem> =
        if (forceRefresh) {
            val favoriteCoins = manager.getAll()
            var marketItems = listOf<MarketItem>()
            if (favoriteCoins.isNotEmpty()) {
                val favoriteCoinUids = favoriteCoins.map { it.coinUid }
                marketItems = marketKit.marketInfosSingle(favoriteCoinUids, currency.code, "watchlist").blockingGet()
                    .map { marketInfo ->
                        MarketItem.createFromCoinMarket(marketInfo, currency)
                    }
            }
            cache = marketItems
            marketItems
        } else {
            cache
        }

    fun get(
        sortingField: SortingField,
        currency: Currency,
        forceRefresh: Boolean
    ): Single<List<MarketItem>> =
        Single.create { emitter ->
            try {
                val marketItems = getFavorites(forceRefresh, currency)
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
