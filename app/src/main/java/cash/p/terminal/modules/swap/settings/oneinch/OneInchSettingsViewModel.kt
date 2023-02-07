package cash.p.terminal.modules.swap.settings.oneinch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.modules.swap.oneinch.OneInchTradeService
import cash.p.terminal.modules.swap.settings.SwapSettingsModule.SwapSettingsError
import cash.p.terminal.modules.swap.settings.oneinch.OneInchSwapSettingsModule.State
import io.reactivex.disposables.CompositeDisposable

class OneInchSettingsViewModel(
        private val service: OneInchSettingsService,
        private val tradeService: OneInchTradeService
) : ViewModel() {

    var buttonState by mutableStateOf(Pair(Translator.getString(R.string.SwapSettings_Apply), true))
        private set

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
                buttonState = Pair(Translator.getString(R.string.SwapSettings_Apply), true)
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
                    buttonState = Pair(it, false)
                }
            }
        }
    }

    override fun onCleared() {
        disposable.clear()
    }

    fun onDoneClick(): Boolean {
        return when (val state = service.state) {
            is State.Valid -> {
                tradeService.swapSettings = state.swapSettings
                true
            }
            is State.Invalid -> {
                false
            }
        }
    }

    sealed class ActionState {
        object Enabled : ActionState()
        class Disabled(val title: String) : ActionState()
    }
}
