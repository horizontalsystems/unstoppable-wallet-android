package io.horizontalsystems.bankwallet.modules.swap.settings.oneinch

import android.util.Range
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.swap.settings.IRecipientAddressService
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.InvalidSlippageType
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettingsError
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSwapSettingsModule.OneInchSwapSettings
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSwapSettingsModule.State
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal
import io.horizontalsystems.ethereumkit.models.Address as EthAddress

class OneInchSettingsService(
        swapSettings: OneInchSwapSettings
) : IRecipientAddressService, Clearable {

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

    var slippage: BigDecimal = swapSettings.slippage
        set(value) {
            field = value
            sync()
        }

//    var deadline: Long = tradeOptions.ttl
//        set(value) {
//            field = value
//            sync()
//        }

    var recipient: Address? = swapSettings.recipient
        set(value) {
            field = value
            sync()
        }

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

    override val initialAddress: Address?
        get() {
            val state = state
            if (state is State.Valid) {
                return state.swapSettings.recipient
            }

            return null
        }

    override val recipientAddressError: Throwable?
        get() = errors.find { it is SwapSettingsError.InvalidAddress }

    override val recipientAddressErrorObservable: Observable<Unit> = errorsObservable.map { errors ->
        errors.find { it is SwapSettingsError.InvalidAddress }
    }

    override fun setRecipientAddress(address: Address?) {
        recipient = address
    }

    override fun setRecipientAmount(amount: BigDecimal) {
    }

    override fun clear() = Unit

    companion object {
        //        val recommendedSlippageBounds = Range(BigDecimal("0.1"), BigDecimal("1"))
//        val recommendedDeadlineBounds = Range(600L, 1800L)
        private val limitSlippageBounds = Range(BigDecimal("0.01"), BigDecimal("20"))
    }
}
