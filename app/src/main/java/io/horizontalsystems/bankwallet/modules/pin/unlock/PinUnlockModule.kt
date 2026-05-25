package io.horizontalsystems.bankwallet.modules.pin.unlock

object PinUnlockModule {

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