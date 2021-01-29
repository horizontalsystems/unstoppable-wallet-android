package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.xrateskit.entities.CoinMarket
import io.reactivex.Observable
import io.reactivex.Single

interface IMarketListDataSource {
    val sortingFields: Array<SortingField>
    val dataUpdatedAsync: Observable<Unit>

    fun getListAsync(currencyCode: String): Single<List<CoinMarket>>
}
