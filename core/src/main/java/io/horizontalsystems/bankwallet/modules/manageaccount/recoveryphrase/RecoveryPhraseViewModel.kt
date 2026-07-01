package io.horizontalsystems.bankwallet.modules.manageaccount.recoveryphrase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType

class RecoveryPhraseViewModel(account: Account) : ViewModel() {
    val words: List<String>
    private val seed: ByteArray?

    var passphrase by mutableStateOf("")
        private set

    var wordsNumbered by mutableStateOf<List<RecoveryPhraseModule.WordNumbered>>(listOf())
        private set

    init {
        if (account.type is AccountType.Mnemonic) {
            words = account.type.words
            wordsNumbered = words.mapIndexed { index, word ->
                RecoveryPhraseModule.WordNumbered(word, index + 1)
            }
            passphrase = account.type.passphrase
            seed = account.type.seed
        } else {
            words = listOf()
            seed = null
        }
    }

}
