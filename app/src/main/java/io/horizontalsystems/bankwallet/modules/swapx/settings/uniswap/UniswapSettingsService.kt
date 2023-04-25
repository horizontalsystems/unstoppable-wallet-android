package cash.p.terminal.modules.swapx.settings.uniswap

import android.util.Range
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.swapx.settings.IRecipientAddressService
import cash.p.terminal.modules.swapx.settings.ISwapDeadlineService
import cash.p.terminal.modules.swapx.settings.ISwapSlippageService
import cash.p.terminal.modules.swapx.settings.SwapSettingsModule
import cash.p.terminal.modules.swapx.settings.uniswap.UniswapSettingsModule.State
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.Optional

class UniswapSettingsService(
    recipientAddress: Address?
) : IRecipientAddressService, ISwapSlippageService, ISwapDeadlineService {

    var state: State = State.Valid(SwapTradeOptions(recipient = recipientAddress))
        private set(value) {
            field = value
            stateObservable.onNext(value)
        }

    val stateObservable = BehaviorSubject.createDefault<State>(State.Invalid)
    private val errorsObservable = BehaviorSubject.createDefault<List<Throwable>>(listOf())

    var errors: List<Throwable> = listOf()
        private set(value) {
            field = value
            errorsObservable.onNext(value)
        }

    private val limitSlippageBounds = Range(BigDecimal("0.01"), BigDecimal("50"))
    private val usualHighestSlippage = BigDecimal(5)
    private var slippage: BigDecimal = TradeOptions.defaultAllowedSlippage

    override val initialSlippage: BigDecimal?
        get() = state.let {
            if (it is State.Valid && it.tradeOptions.allowedSlippage.compareTo(defaultSlippage) != 0) {
                it.tradeOptions.allowedSlippage.stripTrailingZeros()
            } else {
                null
            }
        }

    override val defaultSlippage = BigDecimal("0.5")

    override val recommendedSlippages = listOf(BigDecimal("0.1"), BigDecimal("1"))

    override val slippageError: Throwable?
        get() = getSlippageError(errors)

    override val unusualSlippage get() = usualHighestSlippage < slippage

    override val slippageChangeObservable = PublishSubject.create<Unit>()

    override fun setSlippage(value: BigDecimal) {
        slippage = value
        sync()
        slippageChangeObservable.onNext(Unit)
    }

    private fun getSlippageError(errors: List<Throwable>): Throwable? {
        return errors.find { it is SwapSettingsModule.SwapSettingsError.InvalidSlippage }
    }

    private var deadline: Long = TradeOptions.defaultTtl

    override val initialDeadline: Long?
        get() = state.let {
            if (it is State.Valid && it.tradeOptions.ttl != defaultDeadline) {
                it.tradeOptions.ttl
            } else {
                null
            }
        }

    override val defaultDeadline: Long
        get() = TradeOptions.defaultTtl

    override val recommendedDeadlineBounds = Range(600L, 1800L)

    override val deadlineError: Throwable?
        get() = getDeadlineError(errors)

    override val deadlineErrorObservable: Observable<Optional<Throwable>>
        get() = errorsObservable.map { errors -> Optional.ofNullable(getDeadlineError(errors)) }

    private fun getDeadlineError(errors: List<Throwable>): Throwable? {
        return errors.find { it is SwapSettingsModule.SwapSettingsError.ZeroDeadline }
    }

    override fun setDeadline(value: Long) {
        deadline = value
        sync()
    }

    private var recipient: Address? = recipientAddress
    private var recipientError: Throwable? = null
        set(value) {
            field = value

            val state = if (value == null) {
                DataState.Success(Unit)
            } else {
                DataState.Error(value)
            }
            recipientAddressState.onNext(state)
        }

    override val initialAddress: Address?
        get() {
            val state = state
            if (state is State.Valid) {
                return state.tradeOptions.recipient
            }

            return null
        }

    override val recipientAddressState = BehaviorSubject.create<DataState<Unit>>()

    override fun setRecipientAddress(address: Address?) {
        recipient = address
        sync()
    }

    override fun setRecipientAddressWithError(address: Address?, error: Throwable?) {
        recipientError = error
        recipient = address
        sync()
    }

    override fun setRecipientAmount(amount: BigDecimal) {
    }

    init {
        sync()
    }

    private fun sync() {
        val tradeOptions = SwapTradeOptions()

        val errs = mutableListOf<Exception>()

        tradeOptions.recipient = recipient
        recipientError?.let {
            errs.add(SwapSettingsModule.SwapSettingsError.InvalidAddress)
        }

        when {
            slippage.compareTo(BigDecimal.ZERO) == 0 -> {
                errs.add(SwapSettingsModule.SwapSettingsError.ZeroSlippage)
            }

            slippage > limitSlippageBounds.upper -> {
                errs.add(SwapSettingsModule.SwapSettingsError.InvalidSlippage(SwapSettingsModule.InvalidSlippageType.Higher(limitSlippageBounds.upper)))
            }

            slippage < limitSlippageBounds.lower -> {
                errs.add(SwapSettingsModule.SwapSettingsError.InvalidSlippage(SwapSettingsModule.InvalidSlippageType.Lower(limitSlippageBounds.lower)))
            }

            else -> {
                tradeOptions.allowedSlippage = slippage
            }
        }

        if (deadline != 0L) {
            tradeOptions.ttl = deadline
        } else {
            errs.add(SwapSettingsModule.SwapSettingsError.ZeroDeadline)
        }

        errors = errs

        state = if (errs.isEmpty()) {
            State.Valid(tradeOptions)
        } else {
            State.Invalid
        }
    }
}