package io.horizontalsystems.bankwallet.modules.rooteddevice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.utils.RootUtil
import javax.inject.Inject

@HiltViewModel
class RootedDeviceViewModel @Inject constructor(
    private val localStorage: ILocalStorage,
    rootUtil: RootUtil,
) : ViewModel() {

    var showRootedDeviceWarning by mutableStateOf(!localStorage.ignoreRootedDeviceWarning && rootUtil.isRooted)
        private set

    fun ignoreRootedDeviceWarning() {
        localStorage.ignoreRootedDeviceWarning = true
        showRootedDeviceWarning = false
    }
}
