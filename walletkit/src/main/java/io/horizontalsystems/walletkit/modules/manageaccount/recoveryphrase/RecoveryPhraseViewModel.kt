package io.horizontalsystems.walletkit.modules.manageaccount.recoveryphrase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType

class RecoveryPhraseViewModel(account: Account) : ViewModel() {
    val words: List<String>
    private val seed: ByteArray?

    var passphrase by mutableStateOf("")
        private set

    var wordsNumbered by mutableStateOf<List<RecoveryPhraseModule.WordNumbered>>(listOf())
        private set

    init {
        when (val type = account.type) {
            is AccountType.Mnemonic -> {
                words = type.words
                wordsNumbered = words.mapIndexed { index, word ->
                    RecoveryPhraseModule.WordNumbered(word, index + 1)
                }
                passphrase = type.passphrase
                seed = type.seed
            }
            is AccountType.MoneroMnemonic -> {
                words = type.words
                wordsNumbered = words.mapIndexed { index, word ->
                    RecoveryPhraseModule.WordNumbered(word, index + 1)
                }
                passphrase = type.passphrase
                seed = null
            }
            else -> {
                words = listOf()
                seed = null
            }
        }
    }

}
