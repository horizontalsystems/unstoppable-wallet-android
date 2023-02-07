package cash.p.terminal.modules.rooteddevice

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.ILocalStorage

class RootedDeviceViewModel(private val localStorage: ILocalStorage): ViewModel() {

    fun ignoreRootedDeviceWarning() {
        localStorage.ignoreRootedDeviceWarning = true
    }

}
