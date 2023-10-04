package io.horizontalsystems.bankwallet.modules.pin.set

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.pin.set.PinSetModule.PinSetViewState
import io.horizontalsystems.bankwallet.modules.pin.set.PinSetModule.SetStage.Confirm
import io.horizontalsystems.bankwallet.modules.pin.set.PinSetModule.SetStage.Enter
import io.horizontalsystems.core.IPinComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinSetViewModel(
    private val pinComponent: IPinComponent,
    private val forDuress: Boolean,
) : ViewModel() {

    private var enteredPin = ""
    private var submittedPin = ""

    private var stage = Enter
    private var finished = false
    private var reverseSlideAnimation = false
    private var error: String? = null

    var uiState by mutableStateOf(
        PinSetViewState(
            stage = stage,
            enteredCount = enteredPin.length,
            finished = finished,
            reverseSlideAnimation = reverseSlideAnimation,
            error = error,
        )
    )
        private set


    fun onDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            emitState()
        }
    }

    private fun emitState(delayTime: Long = 0) {
        viewModelScope.launch {
            delay(delayTime)
            uiState = PinSetViewState(
                stage = stage,
                enteredCount = enteredPin.length,
                finished = finished,
                reverseSlideAnimation = reverseSlideAnimation,
                error = error,
            )
        }
    }

    fun finished() {
        finished = false
        emitState()
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
                if (pinComponent.isUnique(enteredPin, forDuress)) {
                    submittedPin = enteredPin

                    enteredPin = ""

                    stage = Confirm
                    emitState(500)
                } else {
                    enteredPin = ""

                    error = Translator.getString(R.string.PinSet_ErrorPinInUse)
                    emitState()
                }
            }
            submittedPin.isNotEmpty() -> {
                if (submittedPin == enteredPin) {
                    try {
                        if (forDuress) {
                            pinComponent.setDuressPin(submittedPin)
                        } else {
                            pinComponent.setPin(submittedPin)
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
        emitState(500)
    }

}
