package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import android.util.Range
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.ISwapTradeOptionsService.*
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal
import io.horizontalsystems.ethereumkit.models.Address as EthAddress

class SwapTradeOptionsService(tradeOptions: SwapTradeOptions) : IRecipientAddressService, Clearable {

    var state: State = State.Valid(tradeOptions)
        private set(value) {
            field = value
            stateObservable.onNext(value)
        }

    val stateObservable = BehaviorSubject.createDefault<State>(State.Invalid)
    val errorsObservable = BehaviorSubject.createDefault<List<Throwable>>(listOf())

    var errors: List<Throwable> = listOf()
        private set(value) {
            field = value
            errorsObservable.onNext(value)
        }

    var slippage: BigDecimal = tradeOptions.allowedSlippage
        set(value) {
            field = value
            sync()
        }

    var deadline: Long = tradeOptions.ttl
        set(value) {
            field = value
            sync()
        }

    var recipient: Address? = tradeOptions.recipient
        set(value) {
            field = value
            sync()
        }

    init {
        sync()
    }

    private fun sync() {
        val tradeOptions = SwapTradeOptions()

        val errs = mutableListOf<Exception>()

        recipient?.let {
            if (it.hex.isNotEmpty()) {
                try {
                    EthAddress(it.hex)
                    tradeOptions.recipient = it
                } catch (err: Exception) {
                    errs.add(TradeOptionsError.InvalidAddress)
                }
            }
        }

        if (slippage.compareTo(BigDecimal.ZERO) == 0) {
            errs.add(TradeOptionsError.ZeroSlippage)
        } else if (slippage > limitSlippageBounds.upper) {
            errs.add(TradeOptionsError.InvalidSlippage(InvalidSlippageType.Higher(limitSlippageBounds.upper)))
        } else if (slippage < limitSlippageBounds.lower) {
            errs.add(TradeOptionsError.InvalidSlippage(InvalidSlippageType.Lower(limitSlippageBounds.lower)))
        } else {
            tradeOptions.allowedSlippage = slippage
        }

        if (deadline != 0L) {
            tradeOptions.ttl = deadline
        } else {
            errs.add(TradeOptionsError.ZeroDeadline)
        }

        errors = errs

        state = if (errs.isEmpty()) {
            State.Valid(tradeOptions)
        } else {
            State.Invalid
        }
    }

    override val initialAddress: Address?
        get() {
            val state = state
            if (state is State.Valid) {
                return state.tradeOptions.recipient
            }

            return null
        }

    override var error: Throwable? = null
        get() = errors.find { it is TradeOptionsError.InvalidAddress }

    override val errorObservable: Observable<Unit> = errorsObservable.map { errors ->
        errors.find { it is TradeOptionsError.InvalidAddress }
    }

    override fun set(address: Address?) {
        recipient = address
    }

    override fun set(amount: BigDecimal) {
    }

    override fun clear() = Unit

    companion object {
        val recommendedSlippageBounds = Range(BigDecimal("0.1"), BigDecimal("1"))
        val recommendedDeadlineBounds = Range(600L, 1800L)
        val limitSlippageBounds = Range(BigDecimal("0.01"), BigDecimal("20"))
    }
}
