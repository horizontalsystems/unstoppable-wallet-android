package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.xrateskit.entities.CoinData
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class MarketSearchService(private val xRateManager: IRateManager) : Clearable {

    var query: String = ""
        set(value) {
            field = value

            fetch()
        }

    val itemsAsync = BehaviorSubject.createDefault(Optional.empty<List<CoinData>>())

    private fun fetch() {
        val queryTrimmed = query.trim()

        if (queryTrimmed.count() < 2) {
            itemsAsync.onNext(Optional.empty())
        } else {
            itemsAsync.onNext(Optional.of(xRateManager.searchCoins(queryTrimmed)))
        }
    }

    override fun clear() = Unit
}