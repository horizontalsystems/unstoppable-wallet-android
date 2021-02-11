package io.horizontalsystems.bankwallet.modules.market.favorites

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.list.IMarketListFetcher
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.Single

class MarketFavoritesService(
        private val rateManager: IRateManager,
        private val marketFavoritesManager: MarketFavoritesManager
) : IMarketListFetcher {

    override val dataUpdatedAsync: Observable<Unit>
        get() = marketFavoritesManager.dataUpdatedAsync

    override fun fetchAsync(currency: Currency): Single<List<MarketItem>> {
        return Single.fromCallable {
            marketFavoritesManager.getAll().map { it.code }
        }
                .flatMap { coinCodes ->
                    rateManager.getCoinMarketList(coinCodes, currency.code)
                }
                .map {
                    it.mapIndexed { index, topMarket ->
                        MarketItem.createFromCoinMarket(topMarket, currency, null)
                    }
                }
    }
}