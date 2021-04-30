package io.horizontalsystems.bankwallet.modules.rooteddevice

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.SingleLiveEvent

class RootedDeviceViewModel(private val localStorage: ILocalStorage): ViewModel() {

    val openMainActivity = SingleLiveEvent<Void>()

    fun ignoreRootedDeviceWarningButtonClicked() {
        localStorage.ignoreRootedDeviceWarning = true
        openMainActivity.call()
    }

}
