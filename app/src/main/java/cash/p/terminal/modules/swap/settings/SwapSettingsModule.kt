package cash.p.terminal.modules.swap.settings

import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.DataState
import cash.p.terminal.ui.compose.components.FormsInputStateWarning
import java.math.BigDecimal

object SwapSettingsModule {

    sealed class InvalidSlippageType {
        class Lower(val min: BigDecimal) : InvalidSlippageType()
        class Higher(val max: BigDecimal) : InvalidSlippageType()
    }

    sealed class SwapSettingsError : Exception() {
        object ZeroSlippage : SwapSettingsError() {
            override fun getLocalizedMessage() = Translator.getString(R.string.SwapSettings_Error_SlippageZero)
        }

        object ZeroDeadline : SwapSettingsError() {
            override fun getLocalizedMessage() = Translator.getString(R.string.SwapSettings_Error_DeadlineZero)
        }

        class InvalidSlippage(val invalidSlippageType: InvalidSlippageType) : SwapSettingsError() {
            override fun getLocalizedMessage(): String {
                return when (invalidSlippageType) {
                    is InvalidSlippageType.Lower -> Translator.getString(R.string.SwapSettings_Error_SlippageTooLow)
                    is InvalidSlippageType.Higher -> Translator.getString(R.string.SwapSettings_Error_SlippageTooHigh, invalidSlippageType.max)
                }
            }
        }

        object InvalidAddress : SwapSettingsError() {
            override fun getLocalizedMessage(): String {
                return Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
            }
        }
    }

    fun getState(caution: Caution?) = when (caution?.type) {
        Caution.Type.Error -> DataState.Error(Exception(caution.text))
        Caution.Type.Warning -> DataState.Error(FormsInputStateWarning(caution.text))
        null -> null
    }


}
