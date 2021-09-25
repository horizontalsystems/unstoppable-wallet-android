package io.horizontalsystems.bankwallet.modules.swap.coincard

import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.AmountType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

class SwapToCoinCardService(
    private val service: SwapMainModule.ISwapService,
    private val tradeService: SwapMainModule.ISwapTradeService,
    private val coinProvider: SwapCoinProvider
) : ISwapCoinCardService {
    private val amountType: AmountType = AmountType.ExactTo

    override val isEstimated: Boolean
        get() = tradeService.amountType != amountType

    override val amount: BigDecimal?
        get() = tradeService.amountTo

    override val coin: PlatformCoin?
        get() = tradeService.coinTo

    override val balance: BigDecimal?
        get() = service.balanceTo

    override val tokensForSelection: List<SwapMainModule.CoinBalanceItem>
        get() = coinProvider.getCoins()

    override val isEstimatedObservable: Observable<Boolean>
        get() = tradeService.amountTypeObservable.map { it != amountType }

    override val amountObservable: Observable<Optional<BigDecimal>>
        get() = tradeService.amountToObservable

    override val coinObservable: Observable<Optional<PlatformCoin>>
        get() = tradeService.coinToObservable

    override val balanceObservable: Observable<Optional<BigDecimal>>
        get() = service.balanceToObservable

    override val errorObservable: Observable<Optional<Throwable>>
        get() = Observable.just(Optional.empty())

    override fun onChangeAmount(amount: BigDecimal?) {
        tradeService.enterAmountTo(amount)
    }

    override fun onSelectCoin(coin: PlatformCoin) {
        tradeService.enterCoinTo(coin)
    }

}
