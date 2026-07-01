package io.horizontalsystems.bankwallet.modules.send.evm.settings

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.Warning
import kotlinx.coroutines.launch

class SendEvmNonceViewModel(
    private val service: SendEvmNonceService
) : ViewModelUiState<SendEvmNonceViewModel.UiState>() {

    private var nonce: Long? = null
    private var showInConfirmation = false
    private var showInSettings = false
    private var warnings: List<Warning> = emptyList()
    private var errors: List<Throwable> = emptyList()

    init {
        viewModelScope.launch {
            service.stateFlow.collect { state ->
                state.dataOrNull?.let {
                    nonce = it.nonce
                    showInConfirmation = !it.default || it.fixed
                    showInSettings = !it.fixed
                    warnings = it.warnings
                    errors = it.errors
                    emitState()
                }
            }
        }
    }

    override fun createState() = UiState(
        nonce = nonce,
        showInConfirmation = showInConfirmation,
        showInSettings = showInSettings,
        warnings = warnings,
        errors = errors
    )

    fun onEnterNonce(nonce: Long) {
        service.setNonce(nonce)
    }

    fun onIncrementNonce() {
        service.increment()
    }

    fun onDecrementNonce() {
        service.decrement()
    }

    data class UiState(
        val nonce: Long?,
        val showInConfirmation: Boolean,
        val showInSettings: Boolean,
        val warnings: List<Warning>,
        val errors: List<Throwable>
    )
}
