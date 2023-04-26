package io.horizontalsystems.bankwallet.modules.swap.settings.uniswap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.UniswapSettingsModule.State
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class UniswapSettingsViewModel(
    private val service: UniswapSettingsService,
) : ViewModel() {

    var buttonState by mutableStateOf(Pair(Translator.getString(R.string.SwapSettings_Apply), true))
        private set

    val tradeOptions: SwapTradeOptions?
        get() = (service.state as? State.Valid)?.tradeOptions

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                syncAction()
            }.let {
                disposable.add(it)
            }
    }

    private fun syncAction() {
        when (service.state) {
            is State.Valid -> {
                buttonState = Pair(Translator.getString(R.string.SwapSettings_Apply), true)
            }

            State.Invalid -> {
                val error = service.errors.firstOrNull() ?: return
                var errorText: String? = null

                when (error) {
                    is SwapSettingsModule.SwapSettingsError.InvalidAddress -> {
                        errorText = Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
                    }

                    is SwapSettingsModule.SwapSettingsError.ZeroSlippage,
                    is SwapSettingsModule.SwapSettingsError.InvalidSlippage -> {
                        errorText = Translator.getString(R.string.SwapSettings_Error_InvalidSlippage)
                    }

                    is SwapSettingsModule.SwapSettingsError.ZeroDeadline -> {
                        errorText = Translator.getString(R.string.SwapSettings_Error_InvalidDeadline)
                    }
                }

                errorText?.let {
                    buttonState = Pair(it, false)
                }
            }
        }
    }

    override fun onCleared() {
        disposable.clear()
    }

    sealed class ActionState {
        object Enabled : ActionState()
        class Disabled(val title: String) : ActionState()
    }
}