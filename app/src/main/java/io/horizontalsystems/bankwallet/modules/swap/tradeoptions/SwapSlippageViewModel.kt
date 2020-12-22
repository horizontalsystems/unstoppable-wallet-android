package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapService
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.ISwapTradeOptionsService.*
import io.reactivex.disposables.CompositeDisposable

class SwapSlippageViewModel(private val service: SwapTradeOptionsService) : ViewModel(), IVerifiedInputViewModel {

    override val inputFieldButtonItems: List<InputFieldButtonItem>
        get() {
            val bounds = SwapTradeOptionsService.recommendedSlippageBounds
            val lowerBoundTitle = bounds.lower.toPlainString()
            val upperBoundTitle = bounds.upper.toPlainString()
            return listOf(
                    InputFieldButtonItem("$lowerBoundTitle%") {
                        inputFieldValueLiveData.postValue(lowerBoundTitle)
                        setInputFieldValue(lowerBoundTitle)
                    },
                    InputFieldButtonItem("$upperBoundTitle%") {
                        inputFieldValueLiveData.postValue(upperBoundTitle)
                        setInputFieldValue(upperBoundTitle)
                    }
            )
        }

    override val inputFieldPlaceholder = SwapService.defaultSlippage.stripTrailingZeros().toPlainString()
    override val inputFieldValueLiveData = MutableLiveData<String?>()
    override val inputFieldCautionLiveData = MutableLiveData<Caution?>()

    private val disposable = CompositeDisposable()

    init {
        if (service.slippage.state is FieldState.Valid && service.tradeOptions.allowedSlippagePercent.compareTo(SwapService.defaultSlippage) != 0) {
            inputFieldValueLiveData.postValue(service.tradeOptions.allowedSlippagePercent.stripTrailingZeros().toPlainString())
        }

        service.slippage.stateObservable
                .subscribe { state ->
                    var caution: Caution? = null
                    if (state is FieldState.NotValid) {
                        state.error.localizedMessage?.let {
                            caution = Caution(it, Caution.Type.Error)
                        }
                    }

                    inputFieldCautionLiveData.postValue(caution)
                }.let {
                    disposable.add(it)
                }
    }

    override fun onCleared() {
        disposable.clear()
    }

    override fun setInputFieldValue(text: String?) {
        service.slippage.state = FieldState.NotValidated
        service.slippage.value = text?.toBigDecimalOrNull() ?: SwapService.defaultSlippage
    }

    override fun validateInputField() {
        service.validateSlippage()
    }

    override fun inputFieldIsValid(text: String?): Boolean {
        if (text.isNullOrBlank()) return true

        val parsed = text.toBigDecimalOrNull()
        return parsed != null && parsed.scale() <= 2
    }
}