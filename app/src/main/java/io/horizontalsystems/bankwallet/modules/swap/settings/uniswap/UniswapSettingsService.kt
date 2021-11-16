package io.horizontalsystems.bankwallet.modules.swap.settings.uniswap

import android.util.Range
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.settings.IRecipientAddressService
import io.horizontalsystems.bankwallet.modules.swap.settings.ISwapDeadlineService
import io.horizontalsystems.bankwallet.modules.swap.settings.ISwapSlippageService
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.InvalidSlippageType
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettingsError
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.UniswapSettingsModule.State
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*
import io.horizontalsystems.ethereumkit.models.Address as EthAddress

class UniswapSettingsService(
        tradeOptions: SwapTradeOptions
) : IRecipientAddressService, ISwapSlippageService, ISwapDeadlineService, Clearable {

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

    // region ISwapSlippageService
    private val limitSlippageBounds = Range(BigDecimal("0.01"), BigDecimal("50"))
    private val usualHighestSlippage = BigDecimal(5)
    private var slippage: BigDecimal = tradeOptions.allowedSlippage

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
        return errors.find { it is SwapSettingsError.InvalidSlippage }
    }
    //endregion

    //region ISwapDeadlineService
    private var deadline: Long = tradeOptions.ttl

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
        return errors.find { it is SwapSettingsError.ZeroDeadline }
    }

    override fun setDeadline(value: Long) {
        deadline = value
        sync()
    }
    //endregion

    //region IRecipientAddressService
    private var recipient: Address? = tradeOptions.recipient
        set(value) {
            field = value
            sync()
        }

    override val initialAddress: Address?
        get() {
            val state = state
            if (state is State.Valid) {
                return state.tradeOptions.recipient
            }

            return null
        }

    override val recipientAddressError: Throwable?
        get() = getRecipientAddressError(errors)

    override val recipientAddressErrorObservable: Observable<Unit> = errorsObservable.map { errors ->
        getRecipientAddressError(errors)
    }

    override fun setRecipientAddress(address: Address?) {
        recipient = address
    }

    override fun setRecipientAmount(amount: BigDecimal) {
    }

    private fun getRecipientAddressError(errors: List<Throwable>): Throwable? {
        return errors.find { it is SwapSettingsError.InvalidAddress }
    }

    //endregion

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
                    errs.add(SwapSettingsError.InvalidAddress)
                }
            }
        }

        when {
            slippage.compareTo(BigDecimal.ZERO) == 0 -> {
                errs.add(SwapSettingsError.ZeroSlippage)
            }
            slippage > limitSlippageBounds.upper -> {
                errs.add(SwapSettingsError.InvalidSlippage(InvalidSlippageType.Higher(limitSlippageBounds.upper)))
            }
            slippage < limitSlippageBounds.lower -> {
                errs.add(SwapSettingsError.InvalidSlippage(InvalidSlippageType.Lower(limitSlippageBounds.lower)))
            }
            else -> {
                tradeOptions.allowedSlippage = slippage
            }
        }

        if (deadline != 0L) {
            tradeOptions.ttl = deadline
        } else {
            errs.add(SwapSettingsError.ZeroDeadline)
        }

        errors = errs

        state = if (errs.isEmpty()) {
            State.Valid(tradeOptions)
        } else {
            State.Invalid
        }
    }

    override fun clear() = Unit

}
