package io.horizontalsystems.bankwallet.modules.backupkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.showkey.ShowKeyModule

class BackupKeyViewModel(val account: Account) : ViewModel() {

    var passphrase by mutableStateOf("")
        private set

    var wordsNumbered by mutableStateOf<List<ShowKeyModule.WordNumbered>>(listOf())
        private set

    init {
        if (account.type is AccountType.Mnemonic) {
            wordsNumbered = account.type.words.mapIndexed { index, word ->
                ShowKeyModule.WordNumbered(word, index + 1)
            }
            passphrase = account.type.passphrase
        }
    }
}
