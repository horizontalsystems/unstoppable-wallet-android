package io.horizontalsystems.bankwallet.modules.market.overview

import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.MarketOverview
import io.reactivex.Single

class MarketOverviewRepository(
    private val marketKit: MarketKit
) {

    fun get(baseCurrency: Currency): Single<MarketOverview> =
        marketKit.marketOverviewSingle(baseCurrency.code)

}
