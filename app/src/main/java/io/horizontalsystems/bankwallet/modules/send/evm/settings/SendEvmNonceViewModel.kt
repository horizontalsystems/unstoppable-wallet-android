package io.horizontalsystems.bankwallet.modules.send.evm.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SendEvmNonceViewModel(
    private val service: SendEvmNonceService
) : ViewModel() {

    private var nonce: Long? = null
    private var showInConfirmation = false
    private var showInSettings = false

    var uiState by mutableStateOf(UiState(nonce, showInConfirmation, showInSettings))
        private set

    init {
        viewModelScope.launch {
            service.stateFlow.collect { state ->
                state.dataOrNull?.let {
                    nonce = it.nonce
                    showInConfirmation = !it.default || it.fixed
                    showInSettings = !it.fixed

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
        uiState = UiState(nonce, showInConfirmation, showInSettings)
    }

    data class UiState(
        val nonce: Long?,
        val showInConfirmation: Boolean,
        val showInSettings: Boolean
    )
}
