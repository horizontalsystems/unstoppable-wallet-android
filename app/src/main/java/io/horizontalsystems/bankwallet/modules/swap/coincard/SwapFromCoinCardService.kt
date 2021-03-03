package io.horizontalsystems.bankwallet.modules.swap.coincard

import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapService
import io.horizontalsystems.bankwallet.modules.swap.SwapTradeService
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

class SwapFromCoinCardService(
        private val service: SwapService,
        private val tradeService: SwapTradeService,
        private val coinProvider: SwapCoinProvider
) : ISwapCoinCardService {
    private val tradeType: TradeType = TradeType.ExactIn

    override val isEstimated: Boolean
        get() = tradeService.tradeType != tradeType

    override val amount: BigDecimal?
        get() = tradeService.amountFrom

    override val coin: Coin?
        get() = tradeService.coinFrom

    override val balance: BigDecimal?
        get() = service.balanceFrom

    override val tokensForSelection: List<SwapModule.CoinBalanceItem>
        get() = coinProvider.coins(enabledCoins = false)

    override val isEstimatedObservable: Observable<Boolean>
        get() = tradeService.tradeTypeObservable.map { it != tradeType }

    override val amountObservable: Observable<Optional<BigDecimal>>
        get() = tradeService.amountFromObservable

    override val coinObservable: Observable<Optional<Coin>>
        get() = tradeService.coinFromObservable

    override val balanceObservable: Observable<Optional<BigDecimal>>
        get() = service.balanceFromObservable

    override val errorObservable: Observable<Optional<Throwable>>
        get() = service.errorsObservable.map { errors -> errors.firstOrNull { it is SwapService.SwapError.InsufficientBalanceFrom }.let { Optional.ofNullable(it) } }

    override fun onChangeAmount(amount: BigDecimal?) {
        tradeService.enterAmountFrom(amount)
    }

    override fun onSelectCoin(coin: Coin) {
        tradeService.enterCoinFrom(coin)
    }

}
