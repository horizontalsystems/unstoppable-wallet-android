package cash.p.terminal.modules.moneroconfigure

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.usecase.ValidateMoneroHeightUseCase
import cash.p.terminal.modules.enablecoin.restoresettings.BirthdayHeightConfigUiState
import cash.p.terminal.modules.enablecoin.restoresettings.TokenConfig
import cash.p.terminal.strings.helpers.Translator

class MoneroConfigureViewModel(
    private val validateMoneroHeightUseCase: ValidateMoneroHeightUseCase,
) : ViewModel() {

    var uiState by mutableStateOf(
        BirthdayHeightConfigUiState(
            birthdayHeight = "",
            restoreAsNew = true,
        )
    )
        private set

    fun onRestoreNew(restoreNew: Boolean) {
        uiState = uiState.copy(
            restoreAsNew = restoreNew,
        )
    }

    fun setBirthdayHeight(height: String) {
        uiState = uiState.copy(
            birthdayHeight = height,
            errorHeight = null
        )
    }

    fun setInitialConfig(config: TokenConfig?) {
        if (config == null) return

        val isNew = config.restoreAsNew
        uiState = uiState.copy(
            birthdayHeight = config.birthdayHeight.orEmpty(),
            restoreAsNew = isNew,
            errorHeight = null,
            closeWithResult = null
        )
    }

    fun onDoneClick() {
        val heightDetected = if (uiState.restoreAsNew) {
            validateMoneroHeightUseCase.getTodayHeight()
        } else {
            validateMoneroHeightUseCase(uiState.birthdayHeight)
        }
        uiState = uiState.copy(
            closeWithResult = if (heightDetected != -1L) {
                TokenConfig(heightDetected.toString(), uiState.restoreAsNew)
            } else {
                null
            },
            errorHeight = if (heightDetected == -1L) {
                Translator.getString(R.string.invalid_height_format)
            } else {
                null
            }
        )
    }

    fun onClosed() {
        uiState = uiState.copy(closeWithResult = null)
    }
}
