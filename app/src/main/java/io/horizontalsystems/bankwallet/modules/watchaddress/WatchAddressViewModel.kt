package io.horizontalsystems.bankwallet.modules.watchaddress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Address

class WatchAddressViewModel(private val service: WatchAddressService) : ViewModel() {

    var accountCreated by mutableStateOf(false)
        private set

    var submitEnabled by mutableStateOf(false)
        private set

    fun onEnterAddress(v: Address?) {
        service.address = v

        submitEnabled = service.isCreatable
    }

    fun onClickWatch() {
        try {
            service.createAccount()
            accountCreated = true
        } catch (e: Exception) {

        }
    }
}
