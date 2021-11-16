package io.horizontalsystems.bankwallet.modules.swap.coincard

import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

interface ISwapCoinCardService {
    val isEstimated: Boolean
    val amount: BigDecimal?
    val coin: PlatformCoin?
    val balance: BigDecimal?

    val isEstimatedObservable: Observable<Boolean>
    val amountObservable: Observable<Optional<BigDecimal>>
    val coinObservable: Observable<Optional<PlatformCoin>>
    val balanceObservable: Observable<Optional<BigDecimal>>
    val errorObservable: Observable<Optional<Throwable>>
    val amountWarningObservable: Observable<Optional<AmountWarning>>
        get() = Observable.empty()

    fun onChangeAmount(amount: BigDecimal?)
    fun onSelectCoin(coin: PlatformCoin)
}

sealed class AmountWarning {
    class HighPriceImpact(val priceImpact: BigDecimal) : AmountWarning()
}
