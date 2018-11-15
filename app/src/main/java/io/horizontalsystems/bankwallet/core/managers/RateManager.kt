package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.Coin
import io.reactivex.subjects.PublishSubject

class RateManager : IRateManager {
    override val subject: PublishSubject<Void>
        get() = TODO("not implemented")

    override fun rateForCoin(coin: Coin, currencyCode: String): Rate? {
        TODO("not implemented")
    }
}