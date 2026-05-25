package io.horizontalsystems.bankwallet.modules.settings.security.autolock

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage
import javax.inject.Inject

@HiltViewModel
class AutoLockIntervalsViewModel @Inject constructor(
    private val localStorage: ILocalStorage
) : ViewModel() {

    private val autoLockInterval: AutoLockInterval
        get() = localStorage.autoLockInterval

    fun onSelectAutoLockInterval(autoLockInterval: AutoLockInterval) {
        localStorage.autoLockInterval = autoLockInterval
    }

    val intervals = AutoLockInterval.values().map {
        AutoLockModule.AutoLockIntervalViewItem(it, it == autoLockInterval)
    }
}
