package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import android.util.Range
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.util.*

interface ISwapSlippageService {
    val initialSlippage: BigDecimal?
    val defaultSlippage: BigDecimal

    val slippageError: Throwable?
    val slippageErrorObservable: Observable<Optional<Throwable>>

    val recommendedSlippageBounds: Range<BigDecimal>

    fun setSlippage(value: BigDecimal)
}

class SwapSlippageViewModel(
        private val service: ISwapSlippageService
) : ViewModel(), IVerifiedInputViewModel {

    private val disposable = CompositeDisposable()

    override val inputFieldButtonItems: List<InputFieldButtonItem>
        get() {
            val bounds = service.recommendedSlippageBounds
            val lowerBoundTitle = bounds.lower.toPlainString()
            val upperBoundTitle = bounds.upper.toPlainString()

            return listOf(
                    InputFieldButtonItem("$lowerBoundTitle%") {
                        setTextLiveData.postValue(lowerBoundTitle)
                        onChangeText(lowerBoundTitle)
                    },
                    InputFieldButtonItem("$upperBoundTitle%") {
                        setTextLiveData.postValue(upperBoundTitle)
                        onChangeText(upperBoundTitle)
                    }
            )
        }

    override val inputFieldPlaceholder: String?
        get() = service.defaultSlippage.toPlainString()
    override val setTextLiveData = MutableLiveData<String?>()
    override val cautionLiveData = MutableLiveData<Caution?>()
    override val initialValue: String?
        get() = service.initialSlippage?.toPlainString()

    init {
        service.slippageErrorObservable
                .subscribe { sync() }
                .let {
                    disposable.add(it)
                }
        sync()
    }

    private fun sync() {
        val caution = service.slippageError?.localizedMessage?.let { localizedMessage ->
            Caution(localizedMessage, Caution.Type.Error)
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
