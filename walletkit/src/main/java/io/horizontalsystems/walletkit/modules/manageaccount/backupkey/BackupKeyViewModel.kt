package io.horizontalsystems.walletkit.modules.manageaccount.backupkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.modules.manageaccount.recoveryphrase.RecoveryPhraseModule

class BackupKeyViewModel(val account: Account) : ViewModel() {

    var passphrase by mutableStateOf("")
        private set

    var wordsNumbered by mutableStateOf<List<RecoveryPhraseModule.WordNumbered>>(listOf())
        private set

    init {
        when (val type = account.type) {
            is AccountType.Mnemonic -> {
                wordsNumbered = type.words.mapIndexed { index, word ->
                    RecoveryPhraseModule.WordNumbered(word, index + 1)
                }
                passphrase = type.passphrase
            }
            is AccountType.MoneroMnemonic -> {
                wordsNumbered = type.words.mapIndexed { index, word ->
                    RecoveryPhraseModule.WordNumbered(word, index + 1)
                }
                passphrase = type.passphrase
            }
            else -> Unit
        }
    }
}
