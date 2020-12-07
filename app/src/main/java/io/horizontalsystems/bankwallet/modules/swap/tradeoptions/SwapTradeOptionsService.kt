package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import android.util.Range
import io.horizontalsystems.bankwallet.modules.swap.SwapService
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.ISwapTradeOptionsService.TradeOptionsError
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

class SwapTradeOptionsService(tradeOptions: TradeOptions) : ISwapTradeOptionsService {

    override var state: ISwapTradeOptionsService.State = ISwapTradeOptionsService.State.Valid(tradeOptions)
        private set(value) {
            field = value
            stateObservable.onNext(value)
        }

    override val stateObservable = BehaviorSubject.createDefault<ISwapTradeOptionsService.State>(ISwapTradeOptionsService.State.Invalid)
    override val errorsObservable = BehaviorSubject.createDefault<List<TradeOptionsError>>(listOf())

    val recommendedSlippageBounds = Range<BigDecimal>(BigDecimal("0.1"), BigDecimal("1"))
    private val limitSlippageBounds = Range<BigDecimal>(BigDecimal("0.01"), BigDecimal("20"))

    val recommendedDeadlineBounds = Range(600L, 1800L)

    var slippage: BigDecimal = tradeOptions.allowedSlippagePercent
        set(value) {
            field = value
            sync()
        }

    var deadline: Long = tradeOptions.ttl
        set(value) {
            field = value
            sync()
        }

    var recipient: String? = tradeOptions.recipient?.hex
        set(value) {
            field = value
            sync()
        }

    init {
        sync()
    }

    private fun sync() {
        val errors = mutableListOf<TradeOptionsError>()

        var allowedSlippagePercent = SwapService.defaultSlippage

        if (slippage.compareTo(BigDecimal.ZERO) == 0) {
            errors.add(TradeOptionsError.ZeroSlippage)
        } else if (slippage > limitSlippageBounds.upper) {
            errors.add(TradeOptionsError.InvalidSlippage(ISwapTradeOptionsService.InvalidSlippageType.Higher(limitSlippageBounds.upper)))
        } else if (slippage < limitSlippageBounds.lower) {
            errors.add(TradeOptionsError.InvalidSlippage(ISwapTradeOptionsService.InvalidSlippageType.Lower(limitSlippageBounds.lower)))
        } else {
            allowedSlippagePercent = slippage
        }

        val tradeOptions = TradeOptions(allowedSlippagePercent)

        if (deadline != 0L) {
            tradeOptions.ttl = deadline
        } else {
            errors.add(TradeOptionsError.ZeroDeadline)
        }

        recipient?.trim()?.let { recipient ->
            if (recipient.isNotEmpty()) {
                try {
                    tradeOptions.recipient = Address(recipient)
                } catch (e: NumberFormatException) {
                    errors.add(TradeOptionsError.InvalidAddress)
                } catch (e: AddressValidator.AddressValidationException) {
                    errors.add(TradeOptionsError.InvalidAddress)
                }
            }
        }

        errorsObservable.onNext(errors)

        state = if (errors.isEmpty()) {
            ISwapTradeOptionsService.State.Valid(tradeOptions)
        } else {
            ISwapTradeOptionsService.State.Invalid
        }
    }

}
