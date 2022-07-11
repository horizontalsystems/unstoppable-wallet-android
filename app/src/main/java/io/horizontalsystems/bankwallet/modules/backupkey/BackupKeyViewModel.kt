package io.horizontalsystems.bankwallet.modules.backupkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule
import io.horizontalsystems.core.IPinComponent

class BackupKeyViewModel(
    val account: Account,
    pinComponent: IPinComponent
) : ViewModel() {

    private val isPinSet = pinComponent.isPinSet

    var passphrase by mutableStateOf("")
        private set

    var wordsNumbered by mutableStateOf<List<ShowKeyModule.WordNumbered>>(listOf())
        private set

    var viewState by mutableStateOf(BackupKeyModule.ViewState.Warning)
        private set

    var showPinUnlock by mutableStateOf(false)
        private set

    var showKeyConfirmation by mutableStateOf(false)
        private set

    init {
        if (account.type is AccountType.Mnemonic) {
            wordsNumbered = account.type.words.mapIndexed { index, word ->
                ShowKeyModule.WordNumbered(word, index + 1)
            }
            passphrase = account.type.passphrase
        }
    }

    fun onClickShow() {
        if (isPinSet) {
            showPinUnlock = true
        } else {
            viewState = BackupKeyModule.ViewState.MnemonicKey
        }
    }

    fun onClickBackup() {
        showKeyConfirmation = true
    }

    fun pinUnlocked() {
        viewState = BackupKeyModule.ViewState.MnemonicKey
    }

    fun pinUnlockShown() {
        showPinUnlock = false
    }

    fun keyConfirmationShown() {
        showKeyConfirmation = false
    }

}
