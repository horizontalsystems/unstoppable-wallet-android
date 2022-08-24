package io.horizontalsystems.bankwallet.modules.restore.restoremnemonic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
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
    private val wordsManager: WordsManager,
    private val thirdKeyboardStorage: IThirdKeyboard,
    private val helper: RestoreMnemonicHelper
) : ViewModel() {

    private var passphraseEnabled: Boolean = false
    private var passphrase: String = ""
    private var passphraseError: String? = null
    private var words: List<WordItem> = listOf()
    private var invalidWords: List<WordItem> = listOf()
    private var invalidWordRanges: List<IntRange> = listOf()
    private var error: String? = null
    private var accountType: AccountType? = null
    private var wordSuggestions: RestoreMnemonicModule.WordSuggestions? = null

    var uiState by mutableStateOf(
        UiState(
            passphraseEnabled = passphraseEnabled,
            passphraseError = passphraseError,
            invalidWordRanges = invalidWordRanges,
            error = error,
            accountType = accountType,
            wordSuggestions = wordSuggestions,
        )
    )
        private set

    private val regex = Regex("\\S+")
    val defaultName = accountFactory.getNextAccountName()

    val isThirdPartyKeyboardAllowed: Boolean
        get() = CoreApp.thirdKeyboardStorage.isThirdPartyKeyboardAllowed

    private fun emitState() {
        uiState = UiState(
            passphraseEnabled = passphraseEnabled,
            passphraseError = passphraseError,
            invalidWordRanges = invalidWordRanges,
            error = error,
            accountType = accountType,
            wordSuggestions = wordSuggestions,
        )
    }

    fun onTogglePassphrase(enabled: Boolean) {
        passphraseEnabled = enabled
        passphrase = ""
        passphraseError = null

        emitState()
    }

    fun onEnterPassphrase(passphrase: String) {
        this.passphrase = passphrase
        passphraseError = null

        emitState()
    }

    fun onEnterMnemonicPhrase(text: String, cursorPosition: Int) {
        words = wordItems(text)
        invalidWords = words.filter {
            !wordsManager.isWordValid(it.word)
        }
        invalidWordRanges = invalidWords.filter {
            val hasCursor = it.range.contains(cursorPosition - 1)
            !hasCursor || !wordsManager.isWordPartiallyValid(it.word)
        }.map {
            it.range
        }
        wordSuggestions = helper.getWordSuggestions(words, cursorPosition)

        emitState()
    }

    fun onProceed() {
        val words = words.map { it.word }

        when {
            invalidWords.isNotEmpty() -> {
                invalidWordRanges = invalidWords.map { it.range }
            }
            words.size !in (Mnemonic.EntropyStrength.values().map { it.wordCount }) -> {
                error = Translator.getString(R.string.Restore_Error_MnemonicWordCount, words.size)
            }
            passphraseEnabled && passphrase.isBlank() -> {
                passphraseError = Translator.getString(R.string.Restore_Error_EmptyPassphrase)
            }
            else -> {
                try {
                    wordsManager.validateChecksum(words)

                    accountType = AccountType.Mnemonic(words, passphrase)
                    error = null
                } catch (checksumException: Exception) {
                    error = Translator.getString(R.string.Restore_InvalidChecksum)
                }
            }
        }

        emitState()
    }

    fun onSelectCoinsShown() {
        accountType = null

        emitState()
    }

    fun onErrorShown() {
        error = null

        emitState()
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
