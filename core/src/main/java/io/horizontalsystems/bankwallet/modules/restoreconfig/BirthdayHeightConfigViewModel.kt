package io.horizontalsystems.bankwallet.modules.restoreconfig

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig

class BirthdayHeightConfigViewModel : ViewModel() {

    var uiState by mutableStateOf(
        BirthdayHeightUiState(
            birthdayHeight = null,
            restoreAsNew = true,
            restoreAsOld = false,
            doneButtonEnabled = true,
        )
    )
        private set

    fun restoreAsNew() {
        uiState = BirthdayHeightUiState(
            birthdayHeight = null,
            restoreAsNew = true,
            restoreAsOld = false,
            doneButtonEnabled = true,
        )
    }

    fun restoreAsOld() {
        uiState = BirthdayHeightUiState(
            birthdayHeight = null,
            restoreAsNew = false,
            restoreAsOld = true,
            doneButtonEnabled = true
        )
    }

    fun setBirthdayHeight(height: String) {
        uiState = BirthdayHeightUiState(
            birthdayHeight = height,
            restoreAsNew = false,
            restoreAsOld = true,
            doneButtonEnabled = height.isNotBlank()
        )
    }

    fun onDoneClick() {
        uiState = BirthdayHeightUiState(
            birthdayHeight = uiState.birthdayHeight,
            restoreAsNew = uiState.restoreAsNew,
            restoreAsOld = uiState.restoreAsOld,
            doneButtonEnabled = uiState.doneButtonEnabled,
            closeWithResult = BirthdayHeightConfig(uiState.birthdayHeight, uiState.restoreAsNew)
        )
    }

    fun onClosed() {
        uiState = BirthdayHeightUiState(
            birthdayHeight = uiState.birthdayHeight,
            restoreAsNew = uiState.restoreAsNew,
            restoreAsOld = uiState.restoreAsOld,
            doneButtonEnabled = uiState.doneButtonEnabled,
            closeWithResult = null
        )
    }
}

data class BirthdayHeightUiState(
    val birthdayHeight: String?,
    val restoreAsNew: Boolean,
    val restoreAsOld: Boolean,
    val doneButtonEnabled: Boolean,
    val closeWithResult: BirthdayHeightConfig? = null
)
