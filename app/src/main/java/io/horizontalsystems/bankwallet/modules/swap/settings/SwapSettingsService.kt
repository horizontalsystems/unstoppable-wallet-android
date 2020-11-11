package io.horizontalsystems.bankwallet.modules.swap.settings

import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettings
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettingsError
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettingsError.*
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettingsState
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

class SwapSettingsService(
        currentSwapSettings: SwapSettings,
        defaultSwapSettings: SwapSettings
) : SwapSettingsModule.ISwapSettingsService {

    private val allowedSlippageRange: ClosedRange<BigDecimal> = BigDecimal("0.01")..BigDecimal("20.0")
    private val minimumDeadline: Long = 1

    override var slippage: BigDecimal? = currentSwapSettings.slippage
    override val defaultSlippage: BigDecimal = defaultSwapSettings.slippage
    override val recommendedSlippageRange: ClosedRange<BigDecimal> = BigDecimal("0.1")..BigDecimal("1.0")

    override var deadline: Long? = currentSwapSettings.deadline
    override val defaultDeadline: Long = defaultSwapSettings.deadline
    override val recommendedDeadlineRange: ClosedRange<Long> = 10L..30L

    override var recipientAddress: String? = currentSwapSettings.recipientAddress

    override val stateObservable = BehaviorSubject.create<SwapSettingsState>()

    init {
        validateState()
    }

    override fun enterSlippage(slippage: BigDecimal?) {
        this.slippage = slippage
        validateState()
    }

    override fun enterDeadline(deadline: Long?) {
        this.deadline = deadline
        validateState()
    }

    override fun enterRecipientAddress(address: String?) {
        this.recipientAddress = address
        validateState()
    }

    private fun validateState() {
        val errors = mutableListOf<SwapSettingsError>()
        slippage?.let {
            when {
                it < allowedSlippageRange.start -> errors.add(SlippageError.SlippageTooLow(allowedSlippageRange.start))
                it > allowedSlippageRange.endInclusive -> errors.add(SlippageError.SlippageTooHigh(allowedSlippageRange.endInclusive))
                else -> {
                }
            }
        }

        deadline?.let {
            if (it <= 0) {
                errors.add(DeadlineError.DeadlineTooLow(minimumDeadline))
            }
        }

        if (!recipientAddress.isNullOrEmpty()) {
            recipientAddress?.let {
                try {
                    AddressValidator.validate(it)
                } catch (error: Throwable) {
                    errors.add(AddressError.InvalidAddress)
                }
            }
        }

        if (errors.isEmpty()) {
            val validSettings = SwapSettings(
                    slippage = slippage ?: defaultSlippage,
                    deadline = deadline ?: defaultDeadline,
                    recipientAddress = if (recipientAddress.isNullOrEmpty()) null else recipientAddress
            )
            stateObservable.onNext(SwapSettingsState.Valid(validSettings))
        } else {
            stateObservable.onNext(SwapSettingsState.Invalid(errors))
        }
    }

}
