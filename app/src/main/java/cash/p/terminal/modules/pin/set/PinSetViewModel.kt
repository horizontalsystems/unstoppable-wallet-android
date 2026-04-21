package cash.p.terminal.modules.pin.set

import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.managers.TransactionHiddenManager
import cash.p.terminal.modules.pin.PinModule
import cash.p.terminal.modules.pin.PinType
import cash.p.terminal.modules.pin.set.PinSetModule.PinSetViewState
import cash.p.terminal.modules.pin.set.PinSetModule.SetStage.Confirm
import cash.p.terminal.modules.pin.set.PinSetModule.SetStage.Enter
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.modules.pin.hiddenwallet.HiddenWalletPinPolicy
import cash.p.terminal.wallet.managers.UserManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ViewModelUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinSetViewModel(
    private val pinComponent: IPinComponent,
    private val pinType: PinType,
    private val transactionHiddenManager: TransactionHiddenManager,
    private val hiddenWalletPinPolicy: HiddenWalletPinPolicy,
    private val userManager: UserManager,
) : ViewModelUiState<PinSetViewState>() {

    private var enteredPin = ""
    private var submittedPin = ""

    private var stage = Enter
    private var finished = false
    private var reverseSlideAnimation = false
    private var error: String? = null

    override fun createState() = PinSetViewState(
        stage = stage,
        enteredCount = enteredPin.length,
        finished = finished,
        reverseSlideAnimation = reverseSlideAnimation,
        error = error,
    )

    fun onDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            emitState()
        }
    }

    fun finished() {
        finished = false
        emitState()
    }

    private fun isPinUnique(pin: String): Boolean {
        return when (pinType) {
            PinType.DURESS, PinType.REGULAR -> {
                pinComponent.isUnique(pin, pinType == PinType.DURESS)
            }
            PinType.HIDDEN_WALLET -> hiddenWalletPinPolicy.canUse(pin)
            PinType.SECURE_RESET, PinType.DELETE_CONTACTS, PinType.LOG_LOGGING -> pinComponent.getPinLevel(pin) == null
            else -> pinComponent.isUnique(pin, true) && pinComponent.isUnique(pin, false)
        }
    }

    fun onKeyClick(number: Int) {
        if (enteredPin.length >= PinModule.PIN_COUNT) return

        error = null
        reverseSlideAnimation = false
        enteredPin += number.toString()
        emitState()

        if (enteredPin.length != PinModule.PIN_COUNT) return

        when {
            stage == Enter -> {
                if (isPinUnique(enteredPin)) {
                    submittedPin = enteredPin

                    enteredPin = ""

                    stage = Confirm
                    viewModelScope.launch {
                        delay(500)
                        emitState()
                    }
                } else {
                    enteredPin = ""

                    error = Translator.getString(R.string.PinSet_ErrorPinInUse)
                    emitState()
                }
            }

            submittedPin.isNotEmpty() -> {
                if (submittedPin == enteredPin) {
                    try {
                        when (pinType) {
                            PinType.DURESS -> pinComponent.setDuressPin(submittedPin)
                            PinType.REGULAR -> pinComponent.setPin(submittedPin)
                            PinType.HIDDEN_WALLET -> {
                                val pinLevel = pinComponent.setHiddenWalletPin(submittedPin)
                                userManager.setUserLevel(pinLevel)
                            }
                            PinType.SECURE_RESET -> pinComponent.setSecureResetPin(submittedPin)
                            PinType.DELETE_CONTACTS -> pinComponent.setDeleteContactsPin(submittedPin)
                            PinType.LOG_LOGGING -> pinComponent.setLogLoggingPin(submittedPin)
                            else -> {
                                transactionHiddenManager.setSeparatePin(submittedPin)
                            }
                        }

                        finished = true
                        emitState()
                    } catch (ex: Exception) {
                        resetWithError(R.string.PinSet_ErrorFailedToSavePin)
                    }
                } else {
                    resetWithError(R.string.PinSet_ErrorPinsDontMatch)
                }
            }
        }
    }

    private fun resetWithError(errorMessage: Int) {
        submittedPin = ""
        enteredPin = ""

        stage = Enter
        reverseSlideAnimation = true
        error = Translator.getString(errorMessage)
        viewModelScope.launch {
            delay(500)
            emitState()
        }
    }

}
