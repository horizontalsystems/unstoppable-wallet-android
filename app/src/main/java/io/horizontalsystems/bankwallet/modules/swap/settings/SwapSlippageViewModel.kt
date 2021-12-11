package io.horizontalsystems.bankwallet.modules.swap.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

interface ISwapSlippageService {
    val slippageChangeObservable: Observable<Unit>

    val initialSlippage: BigDecimal?
    val defaultSlippage: BigDecimal
    val recommendedSlippages: List<BigDecimal>

    val slippageError: Throwable?
    val unusualSlippage: Boolean

    fun setSlippage(value: BigDecimal)
}

class SwapSlippageViewModel(
        private val service: ISwapSlippageService
) : ViewModel(), IVerifiedInputViewModel {

    private val disposable = CompositeDisposable()

    override val inputFieldButtonItems: List<InputFieldButtonItem>
        get() = service.recommendedSlippages.map {
            val slippageStr = it.toPlainString()
            InputFieldButtonItem("$slippageStr%") {
                setTextLiveData.postValue(slippageStr)
                onChangeText(slippageStr)
            }
        }

    override val inputFieldPlaceholder: String?
        get() = service.defaultSlippage.toPlainString()
    override val setTextLiveData = MutableLiveData<String?>()
    override val cautionLiveData = MutableLiveData<Caution?>()
    override val initialValue: String?
        get() = service.initialSlippage?.toPlainString()

    init {
        service.slippageChangeObservable
                .subscribe { sync() }
                .let {
                    disposable.add(it)
                }
        sync()
    }

    private fun sync() {
        val error = service.slippageError?.localizedMessage

        val caution = when {
            error != null -> Caution(error, Caution.Type.Error)
            service.unusualSlippage -> Caution(
                Translator.getString(R.string.SwapSettings_Warning_UnusualSlippage),
                Caution.Type.Warning
            )
            else -> null
        }
        cautionLiveData.postValue(caution)
    }

    override fun onCleared() {
        disposable.clear()
    }

    override fun onChangeText(text: String?) {
        service.setSlippage(text?.toBigDecimalOrNull() ?: service.defaultSlippage)
    }

    override fun isValid(text: String?): Boolean {
        if (text.isNullOrBlank()) return true

        val parsed = text.toBigDecimalOrNull()
        return parsed != null && parsed.scale() <= 2
    }
}
