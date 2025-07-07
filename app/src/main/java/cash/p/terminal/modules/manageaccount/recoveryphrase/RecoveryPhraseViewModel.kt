package cash.p.terminal.modules.manageaccount.recoveryphrase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType

class RecoveryPhraseViewModel(account: Account) : ViewModel() {
    val words: List<String>
    private val seed: ByteArray?

    var passphrase by mutableStateOf("")
        private set

    var wordsNumbered by mutableStateOf<List<RecoveryPhraseModule.WordNumbered>>(listOf())
        private set

    init {
        when (account.type) {
            is AccountType.Mnemonic -> {
                words = (account.type as AccountType.Mnemonic).words
                wordsNumbered = words.mapIndexed { index, word ->
                    RecoveryPhraseModule.WordNumbered(word, index + 1)
                }
                passphrase = (account.type as AccountType.Mnemonic).passphrase
                seed = (account.type as AccountType.Mnemonic).seed
            }

            is AccountType.MnemonicMonero -> {
                words = (account.type as AccountType.MnemonicMonero).words
                wordsNumbered = words.mapIndexed { index, word ->
                    RecoveryPhraseModule.WordNumbered(word, index + 1)
                }
                seed = null
            }

            else -> {
                words = listOf()
                seed = null
            }
        }
    }

}
