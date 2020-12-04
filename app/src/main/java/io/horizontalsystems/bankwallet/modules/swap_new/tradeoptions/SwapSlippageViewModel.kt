package io.horizontalsystems.bankwallet.modules.swap_new.tradeoptions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.swap_new.SwapService
import io.reactivex.disposables.CompositeDisposable

class SwapSlippageViewModel(private val service: SwapTradeOptionsService) : ViewModel(), IVerifiedInputViewModel {

    override val inputFieldButtonItems: List<InputFieldButtonItem>
        get() {
            val bounds = service.recommendedSlippageBounds
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
        val state = service.state
        if (state is ISwapTradeOptionsService.State.Valid && state.tradeOptions.allowedSlippagePercent.compareTo(SwapService.defaultSlippage) != 0) {
            inputFieldValueLiveData.postValue(state.tradeOptions.allowedSlippagePercent.stripTrailingZeros().toPlainString())
        }

        service.errorsObservable
                .subscribe { errors ->
                    val caution = errors.firstOrNull {
                        it is ISwapTradeOptionsService.TradeOptionsError.InvalidSlippage
                    }?.localizedMessage?.let { localizedMessage ->
                        Caution(localizedMessage, Caution.Type.Error)
                    }

                    inputFieldCautionLiveData.postValue(caution)
                }
                .let {
                    disposable.add(it)
                }


    }

    override fun setInputFieldValue(text: String?) {
        service.slippage = text?.toBigDecimalOrNull() ?: SwapService.defaultSlippage
    }

    override fun inputFieldIsValid(text: String?): Boolean {
        if (text.isNullOrBlank()) return true

        val parsed = text.toBigDecimalOrNull()
        return parsed != null && parsed.scale() <= 2
    }
}