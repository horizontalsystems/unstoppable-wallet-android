package io.horizontalsystems.bankwallet.modules.pin.unlock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.pin.core.ILockoutManager
import io.horizontalsystems.bankwallet.modules.pin.core.LockoutState
import io.horizontalsystems.bankwallet.modules.pin.core.OneTimeTimer
import io.horizontalsystems.bankwallet.modules.pin.core.OneTimerDelegate
import io.horizontalsystems.bankwallet.modules.pin.unlock.PinUnlockModule.PinUnlockViewState
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.helpers.DateHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinUnlockViewModel(
    val cancelButtonVisible: Boolean,
    private val pinComponent: IPinComponent,
    private val lockoutManager: ILockoutManager,
    systemInfoManager: ISystemInfoManager,
    private val timer: OneTimeTimer
) : ViewModel(), OneTimerDelegate {


    var uiState by mutableStateOf(
        PinUnlockViewState(
            title = Translator.getString(R.string.Unlock_Passcode),
            error = null,
            enteredCount = 0,
            fingerScannerEnabled = systemInfoManager.biometricAuthSupported && pinComponent.isBiometricAuthEnabled,
            unlocked = false,
            canceled = false,
            showShakeAnimation = false,
            inputState = PinUnlockModule.InputState.Enabled
        )
    )
        private set

    private var enteredPin = ""

    init {
        timer.delegate = this
        updateLockoutState()
    }

    override fun onFire() {
        updateLockoutState()
    }

    fun onBiometricsUnlock() {
        pinComponent.onUnlock()
        lockoutManager.dropFailedAttempts()
        uiState = uiState.copy(unlocked = true)
    }

    fun onKeyClick(number: Int) {
        if (enteredPin.length < PinModule.PIN_COUNT) {

            enteredPin += number.toString()
            uiState = uiState.copy(
                error = null,
                enteredCount = enteredPin.length
            )

            if (enteredPin.length == PinModule.PIN_COUNT) {
                if (unlock(enteredPin)) {
                    uiState = uiState.copy(unlocked = true)
                } else {
                    uiState = uiState.copy(
                        error = Translator.getString(R.string.Unlock_Incorrect),
                        showShakeAnimation = true
                    )
                    viewModelScope.launch {
                        delay(500)
                        enteredPin = ""
                        uiState = uiState.copy(
                            enteredCount = enteredPin.length,
                            showShakeAnimation = false
                        )
                    }
                }
            }
        }
    }

    fun onDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            uiState = uiState.copy(
                error = null,
                enteredCount = enteredPin.length,
                showShakeAnimation = false
            )
        }
    }

    fun unlocked() {
        uiState = uiState.copy(unlocked = false)
    }

    fun canceled() {
        uiState = uiState.copy(canceled = false)
    }

    fun onShakeAnimationFinish() {
        uiState = uiState.copy(showShakeAnimation = false)
    }

    fun onCancelClick() {
        uiState = uiState.copy(canceled = true)
    }

    private fun updateLockoutState() {
        val state = lockoutManager.currentState
        when (state) {
            is LockoutState.Locked -> timer.schedule(state.until)
            is LockoutState.Unlocked -> {}
        }

        uiState = when (state) {
            is LockoutState.Unlocked -> {
                uiState.copy(inputState = PinUnlockModule.InputState.Enabled)
            }
            is LockoutState.Locked -> {
                uiState.copy(
                    inputState = PinUnlockModule.InputState.Locked(
                        until = DateHelper.getOnlyTime(state.until)
                    )
                )
            }
        }
    }

    private fun unlock(pin: String): Boolean {
        val valid = pinComponent.validate(pin)
        if (valid) {
            pinComponent.onUnlock()
            lockoutManager.dropFailedAttempts()
        } else {
            lockoutManager.didFailUnlock()
            updateLockoutState()
        }

        return valid
    }

}
