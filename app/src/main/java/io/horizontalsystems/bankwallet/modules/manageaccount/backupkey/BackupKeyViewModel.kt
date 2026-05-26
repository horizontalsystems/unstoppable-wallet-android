package io.horizontalsystems.bankwallet.modules.manageaccount.backupkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.manageaccount.recoveryphrase.RecoveryPhraseModule

@HiltViewModel(assistedFactory = BackupKeyViewModel.Factory::class)
class BackupKeyViewModel @AssistedInject constructor(
    @Assisted val account: Account,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(account: Account): BackupKeyViewModel
    }

    var passphrase by mutableStateOf("")
        private set

    var wordsNumbered by mutableStateOf<List<RecoveryPhraseModule.WordNumbered>>(listOf())
        private set

    init {
        if (account.type is AccountType.Mnemonic) {
            wordsNumbered = account.type.words.mapIndexed { index, word ->
                RecoveryPhraseModule.WordNumbered(word, index + 1)
            }
            passphrase = account.type.passphrase
        }
    }
}
