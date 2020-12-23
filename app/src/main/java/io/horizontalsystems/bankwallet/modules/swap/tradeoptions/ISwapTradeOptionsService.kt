package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

interface ISwapTradeOptionsService {
    sealed class InvalidSlippageType {
        class Lower(val min: BigDecimal) : InvalidSlippageType()
        class Higher(val max: BigDecimal) : InvalidSlippageType()
    }

    sealed class TradeOptionsError : Throwable() {
        object ZeroSlippage : TradeOptionsError() {
            override fun getLocalizedMessage() = App.instance.getString(R.string.SwapSettings_Error_SlippageZero)
        }

        object ZeroDeadline : TradeOptionsError() {
            override fun getLocalizedMessage() = App.instance.getString(R.string.SwapSettings_Error_DeadlineZero)
        }

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

    sealed class FieldState {
        object NotValidated : FieldState()
        object Validating : FieldState()
        object Valid : FieldState()
        class NotValid(val error: TradeOptionsError) : FieldState()
    }

    class Field<T>(defaultValue: T, private val onValueChange: () -> Unit) {
        var value: T = defaultValue
            set(newVal) {
                field = newVal
                onValueChange()
            }

        val stateObservable: BehaviorSubject<FieldState> = BehaviorSubject.createDefault(FieldState.Valid)
        var state: FieldState = FieldState.Valid
            set(value) {
                field = value
                stateObservable.onNext(value)
            }
    }
}