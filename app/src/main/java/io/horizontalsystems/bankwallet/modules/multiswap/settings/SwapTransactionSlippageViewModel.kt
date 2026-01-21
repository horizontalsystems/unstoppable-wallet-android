package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.multiswap.providers.IMultiSwapProvider
import java.math.BigDecimal

class SwapTransactionSlippageViewModel(
    private val initialSlippage: BigDecimal
) : ViewModelUiState<SwapTransactionSlippageUiState>() {

    private var slippage: BigDecimal = initialSlippage
    private var caution: CautionViewItem? = null
    private var isValid: Boolean = true

    init {
        validateAndUpdateCaution()
    }

    override fun createState() = SwapTransactionSlippageUiState(
        slippage = slippage,
        caution = caution,
        applyEnabled = isValid,
        resetEnabled = slippage.compareTo(DEFAULT_SLIPPAGE) != 0
    )

    fun onSlippageChange(value: BigDecimal) {
        slippage = value
        validateAndUpdateCaution()
        emitState()
    }

    fun onIncrement() {
        val newValue = slippage + STEP
        if (newValue <= MAX_SLIPPAGE) {
            slippage = newValue
            validateAndUpdateCaution()
            emitState()
        }
    }

    fun onDecrement() {
        val newValue = slippage - STEP
        if (newValue >= MIN_SLIPPAGE) {
            slippage = newValue
            validateAndUpdateCaution()
            emitState()
        }
    }

    fun onReset() {
        slippage = DEFAULT_SLIPPAGE
        validateAndUpdateCaution()
        emitState()
    }

    private fun validateAndUpdateCaution() {
        when {
            slippage < MIN_SLIPPAGE -> {
                isValid = false
                caution = CautionViewItem(
                    title = Translator.getString(R.string.SwapSettings_Error_SlippageZero),
                    text = Translator.getString(R.string.SwapSettings_Error_SlippageTooLow),
                    type = CautionViewItem.Type.Error
                )
            }
            slippage > MAX_SLIPPAGE -> {
                isValid = false
                caution = CautionViewItem(
                    title = Translator.getString(R.string.SwapSettings_SlippageTitle),
                    text = Translator.getString(R.string.SwapSettings_Error_SlippageTooHigh, MAX_SLIPPAGE.toPlainString()),
                    type = CautionViewItem.Type.Error
                )
            }
            slippage > WARNING_THRESHOLD -> {
                isValid = true
                caution = CautionViewItem(
                    title = Translator.getString(R.string.SwapSettings_SlippageTitle),
                    text = Translator.getString(R.string.SwapSettings_Warning_UnusualSlippage),
                    type = CautionViewItem.Type.Warning
                )
            }
            else -> {
                isValid = true
                caution = null
            }
        }
    }

    companion object {
        val DEFAULT_SLIPPAGE: BigDecimal = IMultiSwapProvider.DEFAULT_SLIPPAGE
        val MIN_SLIPPAGE: BigDecimal = BigDecimal("0.01")
        val MAX_SLIPPAGE: BigDecimal = BigDecimal("50")
        val WARNING_THRESHOLD: BigDecimal = BigDecimal("5")
        val STEP: BigDecimal = BigDecimal("0.5")
        const val DECIMALS = 2

        fun init(initialSlippage: BigDecimal): CreationExtras.() -> SwapTransactionSlippageViewModel = {
            SwapTransactionSlippageViewModel(initialSlippage)
        }
    }
}

data class SwapTransactionSlippageUiState(
    val slippage: BigDecimal,
    val caution: CautionViewItem?,
    val applyEnabled: Boolean,
    val resetEnabled: Boolean
)
