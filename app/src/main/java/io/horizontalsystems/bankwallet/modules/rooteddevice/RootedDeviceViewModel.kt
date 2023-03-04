package io.horizontalsystems.bankwallet.modules.rooteddevice

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage

class RootedDeviceViewModel(private val localStorage: ILocalStorage): ViewModel() {

    fun ignoreRootedDeviceWarning() {
        localStorage.ignoreRootedDeviceWarning = true
    }

}
