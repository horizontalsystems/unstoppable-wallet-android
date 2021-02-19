package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import java.math.BigDecimal

interface ISwapTradeOptionsService {

    sealed class InvalidSlippageType {
        class Lower(val min: BigDecimal) : InvalidSlippageType()
        class Higher(val max: BigDecimal) : InvalidSlippageType()
    }

    sealed class TradeOptionsError : Exception() {
        object ZeroSlippage : TradeOptionsError() {
            override fun getLocalizedMessage() = App.instance.localizedContext().getString(R.string.SwapSettings_Error_SlippageZero)
        }

        object ZeroDeadline : TradeOptionsError() {
            override fun getLocalizedMessage() = App.instance.localizedContext().getString(R.string.SwapSettings_Error_DeadlineZero)
        }

        class InvalidSlippage(val invalidSlippageType: InvalidSlippageType) : TradeOptionsError() {
            override fun getLocalizedMessage(): String {
                return when (invalidSlippageType) {
                    is InvalidSlippageType.Lower -> App.instance.localizedContext().getString(R.string.SwapSettings_Error_SlippageTooLow)
                    is InvalidSlippageType.Higher -> App.instance.localizedContext().getString(R.string.SwapSettings_Error_SlippageTooHigh, invalidSlippageType.max)
                }
            }
        }

        object InvalidAddress : TradeOptionsError() {
            override fun getLocalizedMessage(): String {
                return App.instance.localizedContext().getString(R.string.SwapSettings_Error_InvalidAddress)
            }
        }
    }

    sealed class State {
        class Valid(val tradeOptions: SwapTradeOptions) : State()
        object Invalid : State()
    }
}
