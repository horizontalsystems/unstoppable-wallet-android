package cash.p.terminal.modules.market.topcoins

import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.entities.Currency
import cash.p.terminal.wallet.models.TopMovers
import io.reactivex.Single

class MarketTopMoversRepository(
    private val marketKit: MarketKitWrapper
) {

    fun getTopMovers(baseCurrency: Currency): Single<TopMovers> =
        marketKit.topMoversSingle(baseCurrency.code)

}
