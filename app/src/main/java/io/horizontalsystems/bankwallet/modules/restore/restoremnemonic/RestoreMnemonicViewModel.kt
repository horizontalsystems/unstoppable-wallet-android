package io.horizontalsystems.bankwallet.modules.restore.restoremnemonic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.managers.PassphraseValidator
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.restore.restoremnemonic.RestoreMnemonicModule.UiState
import io.horizontalsystems.bankwallet.modules.restore.restoremnemonic.RestoreMnemonicModule.WordItem
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.IThirdKeyboard
import io.horizontalsystems.hdwalletkit.Mnemonic

class RestoreMnemonicViewModel(
    accountFactory: IAccountFactory,
    private val passphraseValidator: PassphraseValidator,
    private val wordsManager: WordsManager,
    private val thirdKeyboardStorage: IThirdKeyboard,
    private val helper: RestoreMnemonicHelper
) : ViewModel() {

    private val regex = Regex("\\S+")
    private val defaultName = accountFactory.getNextAccountName()

    val isThirdPartyKeyboardAllowed: Boolean
        get() = CoreApp.thirdKeyboardStorage.isThirdPartyKeyboardAllowed

    val resolvedName: String
        get() = uiState.name.ifBlank { defaultName }

    var uiState by mutableStateOf(UiState(defaultName = defaultName))
        private set

    fun onEnablePassphrase(enabled: Boolean) {
        uiState = uiState.copy(passphraseEnabled = enabled, passphrase = "", passphraseError = null)
    }

    fun onEnterPassphrase(passphrase: String) {
        uiState = if (passphraseValidator.validate(passphrase)) {
            uiState.copy(passphrase = passphrase, passphraseError = null)
        } else {
            uiState.copy(
                passphrase = passphrase,
                passphraseError = Translator.getString(R.string.CreateWallet_Error_PassphraseForbiddenSymbols)
            )
        }
    }

    fun onEnterMnemonicPhrase(text: String, cursorPosition: Int) {
        val allItems = wordItems(text)
        val invalidItems = allItems.filter {
            !wordsManager.isWordValid(it.word)
        }

        val invalidWordRanges = invalidItems.filter {
            val hasCursor = it.range.contains(cursorPosition - 1)
            !hasCursor || !wordsManager.isWordPartiallyValid(it.word)
        }.map {
            it.range
        }

        uiState = uiState.copy(
            words = allItems,
            invalidWords = invalidItems,
            invalidWordRanges = invalidWordRanges,
            wordSuggestions = helper.getWordSuggestions(allItems, cursorPosition)
        )
    }

    fun onEnterName(name: String) {
        uiState = uiState.copy(name = name)
    }

    fun onProceed() {
        val passphrase = uiState.passphrase
        val words = uiState.words.map { it.word }
        val invalidWords = uiState.invalidWords

        uiState = when {
            invalidWords.isNotEmpty() -> {
                uiState.copy(invalidWordRanges = invalidWords.map { it.range })
            }
            words.size !in (Mnemonic.EntropyStrength.values().map { it.wordCount }) -> {
                uiState.copy(
                    error = Translator.getString(R.string.Restore_Error_MnemonicWordCount, words.size)
                )
            }
            uiState.passphraseError != null -> {
                uiState
            }
            uiState.passphraseEnabled && passphrase.isBlank() -> {
                uiState.copy(
                    passphraseError = Translator.getString(R.string.Restore_Error_EmptyPassphrase)
                )
            }
            else -> {
                try {
                    wordsManager.validateChecksum(words)
                    val accountType = AccountType.Mnemonic(words, passphrase)

                    uiState.copy(error = null, accountType = accountType)

                } catch (checksumException: Exception) {
                    uiState.copy(
                        error = Translator.getString(R.string.Restore_InvalidChecksum)
                    )
                }
            }
        }
    }

    fun onSelectCoinsShown() {
        uiState = uiState.copy(accountType = null)
    }

    fun onErrorShown() {
        uiState = uiState.copy(error = null)
    }

    fun onAllowThirdPartyKeyboard() {
        thirdKeyboardStorage.isThirdPartyKeyboardAllowed = true
    }

    private fun wordItems(text: String): List<WordItem> {
        return regex.findAll(text.lowercase())
            .map { WordItem(it.value, it.range) }
            .toList()
    }
}
