package io.horizontalsystems.bankwallet.modules.watchaddress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WatchAddressViewModel(private val service: WatchAddressService) : ViewModel() {

    var accountCreated by mutableStateOf(false)
        private set

    var error: Throwable? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            service.accountCreatedFlow.collect {
                error = it.errorOrNull

                it.dataOrNull?.let {
                    accountCreated = it
                }
            }
        }
    }

    fun onEnterAddress(v: String) {
        service.address = v
    }

    fun onClickWatch() {
        service.createAccount()
    }
}
