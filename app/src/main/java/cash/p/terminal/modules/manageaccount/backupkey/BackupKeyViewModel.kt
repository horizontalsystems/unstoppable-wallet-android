package cash.p.terminal.modules.manageaccount.backupkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.modules.manageaccount.recoveryphrase.RecoveryPhraseModule
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType

class BackupKeyViewModel(val account: Account) : ViewModel() {

    var passphrase by mutableStateOf("")
        private set

    var showPassphraseBlock by mutableStateOf(true)
        private set

    var wordsNumbered by mutableStateOf<List<RecoveryPhraseModule.WordNumbered>>(listOf())
        private set

    init {
        showPassphraseBlock = account.type is AccountType.Mnemonic

        if (account.type is AccountType.Mnemonic) {
            wordsNumbered = (account.type as AccountType.Mnemonic).words.mapIndexed { index, word ->
                RecoveryPhraseModule.WordNumbered(word, index + 1)
            }
            passphrase = (account.type as AccountType.Mnemonic).passphrase
        } else if (account.type is AccountType.MnemonicMonero) {
            wordsNumbered = (account.type as AccountType.MnemonicMonero).words.mapIndexed { index, word ->
                RecoveryPhraseModule.WordNumbered(word, index + 1)
            }
            passphrase = (account.type as AccountType.MnemonicMonero).password
        }
    }
}
