package io.horizontalsystems.bankwallet.modules.swap.settings.oneinch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettingsError
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSwapSettingsModule.State
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class OneInchSettingsViewModel(
    private val service: OneInchSettingsService,
) : ViewModel() {

    var buttonState by mutableStateOf(Pair(Translator.getString(R.string.SwapSettings_Apply), true))
        private set

    val swapSettings: OneInchSwapSettingsModule.OneInchSwapSettings?
        get() = (service.state as? State.Valid)?.swapSettings

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
            .subscribeIO {
                syncAction()
            }.let {
                disposable.add(it)
            }
    }

    private fun syncAction() {
        when (service.state) {
            is State.Valid -> {
                viewModelScope.launch {
                    buttonState = Pair(Translator.getString(R.string.SwapSettings_Apply), true)
                }
            }

            State.Invalid -> {
                val error = service.errors.firstOrNull() ?: return
                var errorText: String? = null

                when (error) {
                    is SwapSettingsError.InvalidAddress -> {
                        errorText = Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
                    }

                    is SwapSettingsError.ZeroSlippage,
                    is SwapSettingsError.InvalidSlippage -> {
                        errorText = Translator.getString(R.string.SwapSettings_Error_InvalidSlippage)
                    }

                    is SwapSettingsError.ZeroDeadline -> {
                        errorText = Translator.getString(R.string.SwapSettings_Error_InvalidDeadline)
                    }
                }

                errorText?.let {
                    viewModelScope.launch {
                        buttonState = Pair(it, false)
                    }
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
