package cash.p.terminal.tangem.ui.accesscode

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.tangem.R
import cash.p.terminal.ui_compose.entities.DataState

internal class AddAccessCodeViewModel : ViewModel() {

    private val _uiState = mutableStateOf(AddAccessCodeUIState())
    val uiState: State<AddAccessCodeUIState> get() = _uiState

    var code: String = ""
        private set
    private var codeConfirmation: String = ""

    fun onToggleHide() {
        _uiState.value = _uiState.value.copy(
            hideCode = !_uiState.value.hideCode
        )
    }

    fun onChangeCode(code: String) {
        this.code = code
        updateState()
    }

    fun onChangeCodeConfirmation(codeConfirmation: String) {
        this.codeConfirmation = codeConfirmation
        updateState()
    }

    private fun updateState() {
        var dataState: DataState.Error? = null
        var dataStateConfirmation: DataState.Error? = null
        if (code.length < 3) {
            dataState = DataState.Error(
                Exception(Translator.getString(R.string.access_code_must_be_4))
            )
        }
        if (dataState == null && code != codeConfirmation) {
            dataStateConfirmation = DataState.Error(
                Exception(Translator.getString(R.string.access_code_confirmation_does_not_match))
            )
        }
        _uiState.value = _uiState.value.copy(
            dataState = dataState,
            dataStateConfirmation = dataStateConfirmation,
            confirmEnabled = dataState == null && dataStateConfirmation == null
        )
    }
}

internal data class AddAccessCodeUIState(
    val dataState: DataState.Error? = null,
    val dataStateConfirmation: DataState.Error? = null,
    val hideCode: Boolean = true,
    val confirmEnabled: Boolean = false,
    val code: String = ""
)