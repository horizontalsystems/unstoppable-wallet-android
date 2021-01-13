package io.horizontalsystems.bankwallet.modules.market.top

import io.horizontalsystems.xrateskit.entities.CoinMarket
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.reactivex.Observable
import io.reactivex.Single

interface IMarketListDataSource {
    val sortingFields: Array<Field>
    val dataUpdatedAsync: Observable<Unit>

    fun getListAsync(currencyCode: String, fetchDiffPeriod: TimePeriod): Single<List<CoinMarket>>
}
