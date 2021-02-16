package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.Score
import io.horizontalsystems.bankwallet.modules.market.list.IMarketListFetcher
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.util.*

class MarketSearchService(private val xRateManager: IRateManager) : Clearable, IMarketListFetcher {

    private val dataUpdatedSubject = PublishSubject.create<Unit>()

    override val dataUpdatedAsync: Observable<Unit>
        get() = dataUpdatedSubject

    var query: String = ""
        set(value) {
            field = value

            dataUpdatedSubject.onNext(Unit)
        }

    override fun fetchAsync(currency: Currency): Single<List<MarketItem>> {
        if (query.isBlank()) return Single.just(listOf())

        val queryLowercased = query.toLowerCase(Locale.ENGLISH)
        return xRateManager.getTopMarketList(currency.code, 250)
                .map {
                    it.filter {
                        it.coin.title.toLowerCase(Locale.ENGLISH).contains(queryLowercased) ||
                        it.coin.code.toLowerCase(Locale.ENGLISH).contains(queryLowercased)
                    }
                }
                .map { coinMarkets ->
                    coinMarkets.mapIndexed { index, coinMarket ->
                        MarketItem.createFromCoinMarket(coinMarket, currency, Score.Rank(index + 1))
                    }
                }


    }

    override fun clear() = Unit
}