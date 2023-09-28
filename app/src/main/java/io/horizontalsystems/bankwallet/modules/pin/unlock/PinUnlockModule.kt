package io.horizontalsystems.bankwallet.modules.pin.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.pin.core.LockoutManager
import io.horizontalsystems.bankwallet.modules.pin.core.LockoutUntilDateFactory
import io.horizontalsystems.bankwallet.modules.pin.core.OneTimeTimer
import io.horizontalsystems.bankwallet.modules.pin.core.UptimeProvider
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.CurrentDateProvider

object PinUnlockModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val lockoutManager = LockoutManager(
                CoreApp.lockoutStorage, UptimeProvider(), LockoutUntilDateFactory(
                    CurrentDateProvider()
                )
            )
            return PinUnlockViewModel(
                App.pinComponent,
                lockoutManager,
                App.systemInfoManager,
                OneTimeTimer()
            ) as T
        }
    }

    data class PinUnlockViewState(
        val enteredCount: Int,
        val fingerScannerEnabled: Boolean,
        val unlocked: Boolean,
        val showShakeAnimation: Boolean,
        val inputState: InputState
    )

    sealed class InputState {
        class Enabled(val attemptsLeft: Int? = null) : InputState()
        class Locked(val until: String) : InputState()
    }

}