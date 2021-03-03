package io.horizontalsystems.bankwallet.modules.swap.coincard

import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

interface ISwapCoinCardService {
    val isEstimated: Boolean
    val amount: BigDecimal?
    val coin: Coin?
    val balance: BigDecimal?
    val tokensForSelection: List<SwapModule.CoinBalanceItem>

    val isEstimatedObservable: Observable<Boolean>
    val amountObservable: Observable<Optional<BigDecimal>>
    val coinObservable: Observable<Optional<Coin>>
    val balanceObservable: Observable<Optional<BigDecimal>>
    val errorObservable: Observable<Optional<Throwable>>

    fun onChangeAmount(amount: BigDecimal?)
    fun onSelectCoin(coin: Coin)
}
