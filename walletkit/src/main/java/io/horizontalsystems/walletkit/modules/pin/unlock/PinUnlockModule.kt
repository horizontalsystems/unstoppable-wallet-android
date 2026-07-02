package io.horizontalsystems.walletkit.modules.pin.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.CoreApp
import io.horizontalsystems.walletkit.CurrentDateProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.modules.pin.core.LockoutManager
import io.horizontalsystems.walletkit.modules.pin.core.LockoutUntilDateFactory
import io.horizontalsystems.walletkit.modules.pin.core.OneTimeTimer
import io.horizontalsystems.walletkit.modules.pin.core.UptimeProvider

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
                OneTimeTimer(),
                App.localStorage,
            ) as T
        }
    }

    data class PinUnlockViewState(
        val enteredCount: Int,
        val biometricEnabled: Boolean,
        val unlocked: Boolean,
        val showShakeAnimation: Boolean,
        val inputState: InputState
    )

    sealed class InputState {
        class Enabled(val attemptsLeft: Int? = null) : InputState()
        class Locked(val until: String) : InputState()
    }

}