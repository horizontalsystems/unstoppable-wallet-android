package com.quantum.wallet.bankwallet.modules.pin.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.modules.pin.core.LockoutManager
import com.quantum.wallet.bankwallet.modules.pin.core.LockoutUntilDateFactory
import com.quantum.wallet.bankwallet.modules.pin.core.OneTimeTimer
import com.quantum.wallet.bankwallet.modules.pin.core.UptimeProvider
import com.quantum.wallet.core.CoreApp
import com.quantum.wallet.core.CurrentDateProvider

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