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
) : ViewModel() {

    private var enteredPin = ""
    private var submittedPin = ""

    var uiState by mutableStateOf(
        PinSetViewState(
            stage = Enter,
            enteredCount = enteredPin.length,
            finished = false,
            reverseSlideAnimation = false,
            error = null,
        )
    )
        private set


    fun onDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            uiState = uiState.copy(
                enteredCount = enteredPin.length
            )
        }
    }

    fun finished() {
        uiState = uiState.copy(finished = false)
    }

    fun onKeyClick(number: Int) {
        if (enteredPin.length < PinModule.PIN_COUNT) {

            enteredPin += number.toString()
            uiState = uiState.copy(
                enteredCount = enteredPin.length,
                error = null,
                reverseSlideAnimation = false
            )

            if (enteredPin.length == PinModule.PIN_COUNT) {
                if (uiState.stage == Enter) {
                    submittedPin = enteredPin

                    viewModelScope.launch {
                        delay(500)
                        enteredPin = ""
                        uiState = uiState.copy(
                            stage = Confirm,
                            enteredCount = enteredPin.length,
                        )
                    }

                } else if (submittedPin.isNotEmpty()) {
                    if (submittedPin == enteredPin) {
                        try {
                            pinComponent.store(submittedPin)
                            uiState = uiState.copy(finished = true)
                        } catch (ex: Exception) {
                            resetWithError(R.string.PinSet_ErrorFailedToSavePin)
                        }
                    } else {
                        viewModelScope.launch {
                            delay(500)
                            resetWithError(R.string.PinSet_ErrorPinsDontMatch)
                        }
                    }
                }
            }
        }
    }

    private fun resetWithError(errorMessage: Int) {
        submittedPin = ""
        enteredPin = ""
        uiState = uiState.copy(
            stage = Enter,
            enteredCount = enteredPin.length,
            reverseSlideAnimation = true,
            error = Translator.getString(errorMessage),
        )
    }

}
