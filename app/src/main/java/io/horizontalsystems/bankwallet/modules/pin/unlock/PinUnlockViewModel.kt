package io.horizontalsystems.bankwallet.modules.pin.unlock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.pin.core.ILockoutManager
import io.horizontalsystems.bankwallet.modules.pin.core.LockoutManager
import io.horizontalsystems.bankwallet.modules.pin.core.LockoutState
import io.horizontalsystems.bankwallet.modules.pin.core.LockoutUntilDateFactory
import io.horizontalsystems.bankwallet.modules.pin.core.OneTimeTimer
import io.horizontalsystems.bankwallet.modules.pin.core.OneTimerDelegate
import io.horizontalsystems.bankwallet.modules.pin.core.UptimeProvider
import io.horizontalsystems.bankwallet.modules.pin.unlock.PinUnlockModule.PinUnlockViewState
import io.horizontalsystems.core.CurrentDateProvider
import io.horizontalsystems.core.ILockoutStorage
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.helpers.DateHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinUnlockViewModel @Inject constructor(
    private val pinComponent: IPinComponent,
    lockoutStorage: ILockoutStorage,
    private val systemInfoManager: ISystemInfoManager,
    private val localStorage: ILocalStorage,
) : ViewModelUiState<PinUnlockViewState>(), OneTimerDelegate {
    private val lockoutManager: ILockoutManager = LockoutManager(
        lockoutStorage, UptimeProvider(), LockoutUntilDateFactory(CurrentDateProvider())
    )
    private val timer = OneTimeTimer()

    private var attemptsLeft: Int? = null

    var pinRandomized by mutableStateOf(localStorage.pinRandomized)
        private set

    private var enteredCount = 0
    private var biometricEnabled = systemInfoManager.biometricAuthSupported && pinComponent.isBiometricAuthEnabled

    private var unlocked = false
    private var showShakeAnimation = false
    private var inputState: PinUnlockModule.InputState = PinUnlockModule.InputState.Enabled(attemptsLeft)

    init {
        viewModelScope.launch {
            pinComponent.isBiometricAuthEnabledFlow.collect {
                biometricEnabled = systemInfoManager.biometricAuthSupported && pinComponent.isBiometricAuthEnabled
                emitState()
            }
        }
    }

    override fun createState() = PinUnlockViewState(
        enteredCount = enteredCount,
        biometricEnabled = biometricEnabled,
        unlocked = unlocked,
        showShakeAnimation = showShakeAnimation,
        inputState = inputState
    )

    private var enteredPin = ""

    init {
        timer.delegate = this
        updateLockoutState()
    }

    override fun onFire() {
        updateLockoutState()
    }

    fun updatePinRandomized(random: Boolean) {
        localStorage.pinRandomized = random
        pinRandomized = random
    }

    fun onBiometricsUnlock() {
        pinComponent.onBiometricUnlock()
        lockoutManager.dropFailedAttempts()

        unlocked = true
        emitState()
    }

    fun onKeyClick(number: Int) {
        if (enteredPin.length < PinModule.PIN_COUNT) {

            enteredPin += number.toString()

            enteredCount = enteredPin.length
            emitState()

            if (enteredPin.length == PinModule.PIN_COUNT) {
                if (unlock(enteredPin)) {
                    unlocked = true
                    emitState()
                } else {
                    showShakeAnimation = true
                    emitState()

                    viewModelScope.launch {
                        delay(500)
                        enteredPin = ""

                        enteredCount = enteredPin.length
                        showShakeAnimation = false
                        emitState()
                    }
                }
            }
        }
    }

    fun onDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)

            enteredCount = enteredPin.length
            showShakeAnimation = false
            emitState()
        }
    }

    fun unlocked() {
        resetState()
    }

    fun onShakeAnimationFinish() {
        showShakeAnimation = false
        emitState()
    }

    private fun resetState() {
        attemptsLeft = null
        enteredPin = ""

        unlocked = false
        enteredCount = 0
        showShakeAnimation = false
        inputState = PinUnlockModule.InputState.Enabled(attemptsLeft)
        emitState()
    }

    private fun updateLockoutState() {
        when (val state = lockoutManager.currentState) {
            is LockoutState.Unlocked -> {
                attemptsLeft = state.attemptsLeft
                inputState = PinUnlockModule.InputState.Enabled(attemptsLeft)
                emitState()
            }

            is LockoutState.Locked -> {
                timer.schedule(state.until)

                inputState = PinUnlockModule.InputState.Locked(
                    until = DateHelper.getOnlyTime(state.until)
                )
                emitState()
            }
        }
    }

    private fun unlock(pin: String): Boolean {
        if (pinComponent.unlock(pin)) {
            lockoutManager.dropFailedAttempts()
            return true
        } else {
            lockoutManager.didFailUnlock()
            updateLockoutState()
            return false
        }
    }
}
