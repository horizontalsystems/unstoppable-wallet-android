package io.horizontalsystems.bankwallet.modules.sendevm

import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Flowable
import java.math.BigDecimal
import java.util.*

interface IAmountInputService {
    val amount: BigDecimal
    val coin: PlatformCoin?

    val amountObservable: Flowable<BigDecimal>
    val coinObservable: Flowable<Optional<PlatformCoin>>

}
