package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object AddressInputModule {

    class Factory(private val coinCode: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val addressViewModel = AddressViewModel()

            addressViewModel.addAddressHandler(AddressHandlerUdn(coinCode))
            addressViewModel.addAddressHandler(AddressHandlerEvm())

            return addressViewModel as T
        }
    }

}
