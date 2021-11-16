package io.horizontalsystems.bankwallet.modules.swap.settings.oneinch

import android.util.Range
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.settings.IRecipientAddressService
import io.horizontalsystems.bankwallet.modules.swap.settings.ISwapSlippageService
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.InvalidSlippageType
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettingsError
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSwapSettingsModule.OneInchSwapSettings
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSwapSettingsModule.State
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import io.horizontalsystems.ethereumkit.models.Address as EthAddress

class OneInchSettingsService(
        swapSettings: OneInchSwapSettings
) : IRecipientAddressService, ISwapSlippageService, Clearable {

    var state: State = State.Valid(swapSettings)
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

    //region IRecipientAddressService
    private var recipient: Address? = swapSettings.recipient
        set(value) {
            field = value
            sync()
        }

    override val initialAddress: Address?
        get() {
            val state = state
            if (state is State.Valid) {
                return state.swapSettings.recipient
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

    // region ISwapSlippageService
    private val limitSlippageBounds = Range(BigDecimal("0.01"), BigDecimal("50"))
    private val usualHighestSlippage = BigDecimal(5)
    private var slippage: BigDecimal = swapSettings.slippage

    override val initialSlippage: BigDecimal?
        get() = state.let {
            if (it is State.Valid && it.swapSettings.slippage.compareTo(defaultSlippage) != 0) {
                it.swapSettings.slippage.stripTrailingZeros()
            } else {
                null
            }
        }
    override val defaultSlippage = OneInchSwapSettingsModule.defaultSlippage

    override val recommendedSlippages = listOf(BigDecimal("0.1"), BigDecimal("3"))

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
        return errors.firstOrNull {
            it is SwapSettingsError.InvalidSlippage
        }
    }
    //endregion

    init {
        sync()
    }

    private fun sync() {
        val swapSettings = OneInchSwapSettings()

        val errs = mutableListOf<Exception>()

        recipient?.let {
            if (it.hex.isNotEmpty()) {
                try {
                    EthAddress(it.hex)
                    swapSettings.recipient = it
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
                swapSettings.slippage = slippage
            }
        }

        errors = errs

        state = if (errs.isEmpty()) {
            State.Valid(swapSettings)
        } else {
            State.Invalid
        }
    }

    override fun clear() = Unit

}
