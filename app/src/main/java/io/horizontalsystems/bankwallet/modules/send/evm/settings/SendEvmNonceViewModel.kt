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

    var nonce by mutableStateOf<Long?>(null)
        private set

    var default by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            service.stateFlow.collect { state ->
                nonce = state.dataOrNull?.nonce
                default = state.dataOrNull?.default ?: true
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

}
