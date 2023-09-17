package cash.p.terminal.modules.restorelocal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.managers.PassphraseValidator
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.DataState

class RestoreLocalViewModel(
    private val passphraseValidator: PassphraseValidator,
) : ViewModel() {

    private var passphrase = ""

    private var passphraseState: DataState.Error? = null
    private var showButtonSpinner = false
    private var closeScreen = false

    var uiState by mutableStateOf(
        RestoreLocalModule.UiState(
            passphraseState = null,
            showButtonSpinner = showButtonSpinner,
            closeScreen = closeScreen,
        )
    )
        private set

    fun onChangePassphrase(v: String) {
        if (passphraseValidator.validate(v)) {
            passphraseState = null
            passphrase = v
        } else {
            passphraseState = DataState.Error(
                Exception(
                    Translator.getString(R.string.CreateWallet_Error_PassphraseForbiddenSymbols)
                )
            )
        }
        syncState()
    }

    fun onImportClick() {

    }

    fun closeScreenCalled() {
        closeScreen = false
        syncState()
    }

    private fun syncState() {
        uiState = RestoreLocalModule.UiState(
            passphraseState = passphraseState,
            showButtonSpinner = showButtonSpinner,
            closeScreen = closeScreen,
        )
    }

}
