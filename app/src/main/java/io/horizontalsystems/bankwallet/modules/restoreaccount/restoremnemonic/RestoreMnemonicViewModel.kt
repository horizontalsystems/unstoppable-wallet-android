package io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.normalizeNFKD
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestoreMnemonicModule.UiState
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoremnemonic.RestoreMnemonicModule.WordItem
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.IThirdKeyboard
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hdwalletkit.WordList

class RestoreMnemonicViewModel(
    accountFactory: IAccountFactory,
    private val wordsManager: WordsManager,
    private val thirdKeyboardStorage: IThirdKeyboard,
) : ViewModel() {

    val mnemonicLanguages = Language.values().toList()

    private var passphraseEnabled: Boolean = false
    private var passphrase: String = ""
    private var passphraseError: String? = null
    private var wordItems: List<WordItem> = listOf()
    private var invalidWordItems: List<WordItem> = listOf()
    private var invalidWordRanges: List<IntRange> = listOf()
    private var error: String? = null
    private var accountType: AccountType? = null
    private var wordSuggestions: RestoreMnemonicModule.WordSuggestions? = null
    private var language = Language.English
    private var text = ""
    private var cursorPosition = 0
    private var mnemonicWordList = WordList.wordListStrict(language)


    var uiState by mutableStateOf(
        UiState(
            passphraseEnabled = passphraseEnabled,
            passphraseError = passphraseError,
            invalidWordRanges = invalidWordRanges,
            error = error,
            accountType = accountType,
            wordSuggestions = wordSuggestions,
            language = language,
        )
    )
        private set

    private val regex = Regex("\\S+")

    val defaultName = accountFactory.getNextAccountName()
    var accountName: String = defaultName
        get() = field.ifBlank { defaultName }
        private set


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
            language = language,
        )
    }

    private fun processText() {
        wordItems = wordItems(text)
        invalidWordItems = wordItems.filter { !mnemonicWordList.validWord(it.word.normalizeNFKD(), false) }

        val wordItemWithCursor = wordItems.find {
            it.range.contains(cursorPosition - 1)
        }

        val invalidWordItemsExcludingCursoredPartiallyValid = when {
            wordItemWithCursor != null && mnemonicWordList.validWord(wordItemWithCursor.word.normalizeNFKD(), true) -> {
                invalidWordItems.filter { it != wordItemWithCursor }
            }
            else -> invalidWordItems
        }

        invalidWordRanges = invalidWordItemsExcludingCursoredPartiallyValid.map { it.range }
        wordSuggestions = wordItemWithCursor?.let {
            RestoreMnemonicModule.WordSuggestions(it, mnemonicWordList.fetchSuggestions(it.word.normalizeNFKD()))
        }
    }

    fun onTogglePassphrase(enabled: Boolean) {
        passphraseEnabled = enabled
        passphrase = ""
        passphraseError = null
        passphraseError = null

        emitState()
    }

    fun onEnterPassphrase(passphrase: String) {
        this.passphrase = passphrase
        passphraseError = null

        emitState()
    }

    fun onEnterName(name: String) {
        accountName = name
    }

    fun onEnterMnemonicPhrase(text: String, cursorPosition: Int) {
        error = null
        this.text = text
        this.cursorPosition = cursorPosition
        processText()

        emitState()
    }

    fun setMnemonicLanguage(language: Language) {
        this.language = language
        mnemonicWordList = WordList.wordListStrict(language)
        processText()

        emitState()
    }

    fun onProceed() {
        when {
            invalidWordItems.isNotEmpty() -> {
                invalidWordRanges = invalidWordItems.map { it.range }
            }
            wordItems.size !in (Mnemonic.EntropyStrength.values().map { it.wordCount }) -> {
                error = Translator.getString(R.string.Restore_Error_MnemonicWordCount, wordItems.size)
            }
            passphraseEnabled && passphrase.isBlank() -> {
                passphraseError = Translator.getString(R.string.Restore_Error_EmptyPassphrase)
            }
            else -> {
                try {
                    val words = wordItems.map { it.word.normalizeNFKD() }
                    wordsManager.validateChecksumStrict(words)

                    accountType = AccountType.Mnemonic(words, passphrase.normalizeNFKD())
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

    fun onAllowThirdPartyKeyboard() {
        thirdKeyboardStorage.isThirdPartyKeyboardAllowed = true
    }

    private fun wordItems(text: String): List<WordItem> {
        return regex.findAll(text.lowercase())
            .map { WordItem(it.value, it.range) }
            .toList()
    }
}
