package io.horizontalsystems.bankwallet.modules.market.list

import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.Single

interface IMarketListFetcher {
    val dataUpdatedAsync: Observable<Unit>

    fun fetchAsync(currency: Currency): Single<List<MarketItem>>
}
