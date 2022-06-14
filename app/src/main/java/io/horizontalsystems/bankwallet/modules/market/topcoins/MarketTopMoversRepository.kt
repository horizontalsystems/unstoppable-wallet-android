package io.horizontalsystems.bankwallet.modules.market.topcoins

import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.TopMovers
import io.reactivex.Single
import java.lang.Integer.min

class MarketTopMoversRepository(
    private val marketKit: MarketKit
) {

    fun getTopMovers(baseCurrency: Currency): Single<TopMovers> =
        Single.create { emitter ->

            try {
                val topMovers = marketKit.topMoversSingle(baseCurrency.code).blockingGet()

                emitter.onSuccess(topMovers)
            } catch (error: Throwable) {
                emitter.onError(error)
            }
        }

    fun get(
        size: Int,
        sortingField: SortingField,
        limit: Int,
        baseCurrency: Currency
    ): Single<List<MarketItem>> =
        Single.create { emitter ->
            try {
                val marketInfoList = marketKit.marketInfosSingle(size, baseCurrency.code).blockingGet()
                val marketItemList = marketInfoList.map { marketInfo ->
                    MarketItem.createFromCoinMarket(
                        marketInfo,
                        baseCurrency,
                    )
                }

                val sortedMarketItems = marketItemList
                    .subList(0, min(marketInfoList.size, size))
                    .sort(sortingField)
                    .subList(0, min(marketInfoList.size, limit))

                emitter.onSuccess(sortedMarketItems)
            } catch (error: Throwable) {
                emitter.onError(error)
            }
        }

}
