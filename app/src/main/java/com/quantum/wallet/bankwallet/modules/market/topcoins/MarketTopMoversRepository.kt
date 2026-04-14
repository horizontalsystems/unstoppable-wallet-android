package com.quantum.wallet.bankwallet.modules.market.topcoins

import com.quantum.wallet.bankwallet.core.managers.MarketKitWrapper
import com.quantum.wallet.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.TopMovers
import io.reactivex.Single

class MarketTopMoversRepository(
    private val marketKit: MarketKitWrapper
) {

    fun getTopMovers(baseCurrency: Currency): Single<TopMovers> =
        marketKit.topMoversSingle(baseCurrency.code)

}
