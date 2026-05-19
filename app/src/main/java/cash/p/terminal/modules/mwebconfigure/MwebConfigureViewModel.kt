package cash.p.terminal.modules.mwebconfigure

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.managers.LitecoinMwebRestoreHeight
import cash.p.terminal.modules.enablecoin.restoresettings.BirthdayHeightConfigUiState
import cash.p.terminal.modules.enablecoin.restoresettings.TokenConfig
import cash.p.terminal.strings.helpers.Translator

class MwebConfigureViewModel : ViewModel() {

    var uiState by mutableStateOf(
        BirthdayHeightConfigUiState(
            birthdayHeight = "",
            restoreAsNew = true,
        )
    )
        private set

    fun onRestoreNew(restoreNew: Boolean) {
        uiState = uiState.copy(restoreAsNew = restoreNew)
    }

    fun setBirthdayHeight(height: String) {
        uiState = uiState.copy(
            birthdayHeight = height,
            errorHeight = null
        )
    }

    fun setInitialConfig(config: TokenConfig?) {
        if (config == null) return

        uiState = uiState.copy(
            birthdayHeight = config.birthdayHeight.orEmpty(),
            restoreAsNew = config.restoreAsNew,
            errorHeight = null,
            closeWithResult = null
        )
    }

    fun onDoneClick() {
        val height = if (uiState.restoreAsNew) {
            null
        } else {
            LitecoinMwebRestoreHeight.parse(uiState.birthdayHeight)
        }
        val invalidHeight = !uiState.restoreAsNew && height == null

        uiState = uiState.copy(
            closeWithResult = if (invalidHeight) {
                null
            } else {
                TokenConfig(height?.toString(), uiState.restoreAsNew)
            },
            errorHeight = if (invalidHeight) {
                Translator.getString(R.string.invalid_height)
            } else {
                null
            }
        )
    }

    fun onClosed() {
        uiState = uiState.copy(closeWithResult = null)
    }
}
