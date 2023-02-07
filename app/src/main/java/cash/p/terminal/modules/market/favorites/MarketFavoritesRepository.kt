package cash.p.terminal.modules.market.favorites

import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.modules.market.MarketItem
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.market.sort
import cash.p.terminal.entities.Currency
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
                marketItems = marketKit.marketInfosSingle(favoriteCoinUids, currency.code).blockingGet()
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
