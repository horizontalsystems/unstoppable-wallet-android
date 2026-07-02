package io.horizontalsystems.core.modules.market.topcoins

import io.horizontalsystems.core.core.managers.MarketKitWrapper
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.TopMovers
import io.reactivex.Single

class MarketTopMoversRepository(
    private val marketKit: MarketKitWrapper
) {

    fun getTopMovers(baseCurrency: Currency): Single<TopMovers> =
        marketKit.topMoversSingle(baseCurrency.code)

}
