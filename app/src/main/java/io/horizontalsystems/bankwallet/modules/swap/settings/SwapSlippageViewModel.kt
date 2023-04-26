package io.horizontalsystems.bankwallet.modules.swap.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.InputButton
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

    override val inputButtons: List<InputButton>
        get() = service.recommendedSlippages.map {
            val slippageStr = it.toPlainString()
            InputButton("$slippageStr%", slippageStr)
        }

    override val inputFieldPlaceholder: String?
        get() = service.defaultSlippage.toPlainString()
    override val initialValue: String?
        get() = service.initialSlippage?.toPlainString()

    var errorState by mutableStateOf<DataState.Error?>(null)
        private set

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
        errorState = SwapSettingsModule.getState(caution)
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
