package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IRandomProvider
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DataState

class BackupConfirmKeyViewModel(
    private val account: Account,
    private val accountManager: IAccountManager,
    indexesProvider: IRandomProvider
) : ViewModel() {

    private val words: List<String>
    private val passphrase: String

    private var enteredFirstWord: String = ""
    private var enteredSecondWord: String = ""
    private var enteredPassphrase: String = ""

    init {
        if (account.type is AccountType.Mnemonic) {
            words = account.type.words
            passphrase = account.type.passphrase
        } else {
            words = listOf()
            passphrase = ""
        }
    }

    private val indices = indexesProvider.getRandomIndexes(2, words.size)

    val firstInputPrefix = "${indices[0] + 1}."

    val secondInputPrefix = "${indices[1] + 1}."

    var firstInputErrorState by mutableStateOf<DataState.Error?>(null)
        private set

    var secondInputErrorState by mutableStateOf<DataState.Error?>(null)
        private set

    var passphraseErrorState by mutableStateOf<DataState.Error?>(null)
        private set

    val passphraseVisible = passphrase.isNotBlank()

    var successMessage by mutableStateOf<Int?>(null)
        private set

    fun onChangeFirstWord(v: String) {
        enteredFirstWord = v
        firstInputErrorState = null
    }

    fun onChangeSecondWord(v: String) {
        enteredSecondWord = v
        secondInputErrorState = null
    }

    fun onChangePassphrase(v: String) {
        enteredPassphrase = v
        passphraseErrorState = null
    }

    fun onSuccessMessageShown() {
        successMessage = null
    }

    fun onClickDone() {
        validate()

        if (firstInputErrorState == null &&
            secondInputErrorState == null &&
            (passphrase.isBlank() || passphraseErrorState == null)
        ) {
            accountManager.update(account.copy(isBackedUp = true))
            successMessage = R.string.Hud_Text_Done
        }
    }

    private fun validate() {
        firstInputErrorState = when {
            enteredFirstWord.isBlank() -> DataState.Error(Exception(Translator.getString(R.string.BackupConfirmKey_Error_EmptyWord)))
            enteredFirstWord != words[indices[0]] -> DataState.Error(
                Exception(
                    Translator.getString(
                        R.string.BackupConfirmKey_Error_InvalidWord
                    )
                )
            )
            else -> null
        }

        secondInputErrorState = when {
            enteredSecondWord.isBlank() -> DataState.Error(Exception(Translator.getString(R.string.BackupConfirmKey_Error_EmptyWord)))
            enteredSecondWord != words[indices[1]] -> DataState.Error(
                Exception(
                    Translator.getString(
                        R.string.BackupConfirmKey_Error_InvalidWord
                    )
                )
            )
            else -> null
        }

        passphraseErrorState = when {
            enteredPassphrase.isBlank() -> DataState.Error(Exception(Translator.getString(R.string.BackupConfirmKey_Error_EmptyPassphrase)))
            enteredPassphrase != passphrase -> DataState.Error(Exception(Translator.getString(R.string.BackupConfirmKey_Error_InvalidPassphrase)))
            else -> null
        }
    }
}
