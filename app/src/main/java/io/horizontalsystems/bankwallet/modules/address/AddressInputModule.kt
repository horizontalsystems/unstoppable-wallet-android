package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unstoppabledomains.resolution.Resolution

object AddressInputModule {

    class Factory(private val coinCode: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressViewModel(Resolution(), coinCode) as T
        }
    }

}
