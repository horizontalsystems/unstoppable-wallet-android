package io.horizontalsystems.bankwallet.modules.swap.coincard

import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapService
import io.horizontalsystems.bankwallet.modules.swap.SwapTradeService
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

class SwapToCoinCardService(
        private val service: SwapService,
        private val tradeService: SwapTradeService,
        private val coinProvider: SwapCoinProvider
) : ISwapCoinCardService {
    private val tradeType: TradeType = TradeType.ExactOut

    override val isEstimated: Boolean
        get() = tradeService.tradeType != tradeType

    override val amount: BigDecimal?
        get() = tradeService.amountTo

    override val coin: Coin?
        get() = tradeService.coinTo

    override val balance: BigDecimal?
        get() = service.balanceTo

    override val tokensForSelection: List<SwapModule.CoinBalanceItem>
        get() = coinProvider.coins(enabledCoins = false)

    override val isEstimatedObservable: Observable<Boolean>
        get() = tradeService.tradeTypeObservable.map { it != tradeType }

    override val amountObservable: Observable<Optional<BigDecimal>>
        get() = tradeService.amountToObservable

    override val coinObservable: Observable<Optional<Coin>>
        get() = tradeService.coinToObservable

    override val balanceObservable: Observable<Optional<BigDecimal>>
        get() = service.balanceToObservable

    override val errorObservable: Observable<Optional<Throwable>>
        get() = Observable.just(Optional.empty())

    override fun onChangeAmount(amount: BigDecimal?) {
        tradeService.enterAmountTo(amount)
    }

    override fun onSelectCoin(coin: Coin) {
        tradeService.enterCoinTo(coin)
    }

}
