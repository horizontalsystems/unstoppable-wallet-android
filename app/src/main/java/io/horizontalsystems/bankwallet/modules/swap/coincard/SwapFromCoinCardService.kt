package io.horizontalsystems.bankwallet.modules.swap.coincard

import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.AmountType
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

class SwapFromCoinCardService(
        private val service: SwapMainModule.ISwapService,
        private val tradeService: SwapMainModule.ISwapTradeService,
) : ISwapCoinCardService {
    private val amountType: AmountType = AmountType.ExactFrom

    override val isEstimated: Boolean
        get() = tradeService.amountType != amountType

    override val amount: BigDecimal?
        get() = tradeService.amountFrom

    override val token: Token?
        get() = tradeService.tokenFrom

    override val balance: BigDecimal?
        get() = service.balanceFrom

    override val isEstimatedObservable: Observable<Boolean>
        get() = tradeService.amountTypeObservable.map { it != amountType }

    override val amountObservable: Observable<Optional<BigDecimal>>
        get() = tradeService.amountFromObservable

    override val tokenObservable: Observable<Optional<Token>>
        get() = tradeService.tokenFromObservable

    override val balanceObservable: Observable<Optional<BigDecimal>>
        get() = service.balanceFromObservable

    override val errorObservable: Observable<Optional<Throwable>>
        get() = service.errorsObservable.map { errors -> errors.firstOrNull { it is SwapError.InsufficientBalanceFrom }.let { Optional.ofNullable(it) } }

    override fun onChangeAmount(amount: BigDecimal?) {
        tradeService.enterAmountFrom(amount)
    }

    override fun onSelectCoin(token: Token) {
        tradeService.enterTokenFrom(token)
    }

}
