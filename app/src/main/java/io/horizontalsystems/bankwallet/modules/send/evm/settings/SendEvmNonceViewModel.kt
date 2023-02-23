package io.horizontalsystems.bankwallet.modules.send.evm.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.Warning
import kotlinx.coroutines.launch

class SendEvmNonceViewModel(
    private val service: SendEvmNonceService
) : ViewModel() {

    private var nonce: Long? = null
    private var showInConfirmation = false
    private var showInSettings = false
    private var warnings: List<Warning> = emptyList()
    private var errors: List<Throwable> = emptyList()

    var uiState by mutableStateOf(UiState(nonce, showInConfirmation, showInSettings, warnings, errors))
        private set

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

    fun onEnterNonce(nonce: Long) {
        service.setNonce(nonce)
    }

    fun onIncrementNonce() {
        service.increment()
    }

    fun onDecrementNonce() {
        service.decrement()
    }

    private fun emitState() {
        uiState = UiState(nonce, showInConfirmation, showInSettings, warnings, errors)
    }

    data class UiState(
        val nonce: Long?,
        val showInConfirmation: Boolean,
        val showInSettings: Boolean,
        val warnings: List<Warning>,
        val errors: List<Throwable>
    )
}
