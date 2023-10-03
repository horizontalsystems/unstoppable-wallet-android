package io.horizontalsystems.bankwallet.modules.settings.security.autolock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object AutoLockModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AutoLockIntervalsViewModel(App.localStorage) as T
        }
    }

    data class AutoLockIntervalViewItem(val interval: AutoLockInterval, val selected: Boolean)
}

enum class AutoLockInterval(val raw: String, val title: Int) {
    IMMEDIATE("immediate", R.string.SettingsSecurity_AutoLock_Immediate),
    AFTER_1_MIN("1min", R.string.SettingsSecurity_AutoLock_After1Min),
    AFTER_5_MIN("5min", R.string.SettingsSecurity_AutoLock_After5Min),
    AFTER_15_MIN("15min", R.string.SettingsSecurity_AutoLock_After15Min),
    AFTER_30_MIN("30min", R.string.SettingsSecurity_AutoLock_After30Min),
    AFTER_1_HOUR("1hour", R.string.SettingsSecurity_AutoLock_After1Hour);

    val intervalInSeconds: Int
        get() = when (this) {
            IMMEDIATE -> 0
            AFTER_1_MIN -> 60
            AFTER_5_MIN -> 5 * 60
            AFTER_15_MIN -> 15 * 60
            AFTER_30_MIN -> 30 * 60
            AFTER_1_HOUR -> 60 * 60
        }

    companion object {
        fun fromRaw(raw: String): AutoLockInterval? {
            return values().find { it.raw == raw }
        }
    }
}