package io.horizontalsystems.bankwallet.modules.swap_new.tradeoptions

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.Observable
import java.math.BigDecimal

interface ISwapTradeOptionsService {
    val state: State
    val stateObservable: Observable<State>
    val errorsObservable: Observable<List<TradeOptionsError>>

    sealed class InvalidSlippageType {
        class Lower(val min: BigDecimal) : InvalidSlippageType()
        class Higher(val max: BigDecimal) : InvalidSlippageType()
    }

    sealed class TradeOptionsError : Throwable() {
        object ZeroSlippage : TradeOptionsError()
        object ZeroDeadline : TradeOptionsError()

        class InvalidSlippage(val invalidSlippageType: InvalidSlippageType) : TradeOptionsError() {
            override fun getLocalizedMessage(): String {
                return when (invalidSlippageType) {
                    is InvalidSlippageType.Lower -> App.instance.getString(R.string.SwapSettings_Error_SlippageTooLow)
                    is InvalidSlippageType.Higher -> App.instance.getString(R.string.SwapSettings_Error_SlippageTooHigh, invalidSlippageType.max)
                }
            }
        }

        object InvalidAddress : TradeOptionsError() {
            override fun getLocalizedMessage(): String {
                return App.instance.getString(R.string.SwapSettings_Error_InvalidAddress)
            }
        }
    }

    sealed class State {
        class Valid(val tradeOptions: TradeOptions) : State()
        object Invalid : State()
    }

}