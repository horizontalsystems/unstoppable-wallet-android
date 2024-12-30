package cash.p.terminal.modules.restoreaccount.restoremnemonicnonstandard

import cash.p.terminal.R
import cash.p.terminal.core.IAccountFactory
import io.horizontalsystems.core.ViewModelUiState
import cash.p.terminal.core.managers.WordsManager
import cash.p.terminal.modules.restoreaccount.restoremnemonic.RestoreMnemonicModule
import cash.p.terminal.modules.restoreaccount.restoremnemonicnonstandard.RestoreMnemonicNonStandardModule.UiState
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.IThirdKeyboard
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hdwalletkit.WordList

class RestoreMnemonicNonStandardViewModel(
    accountFactory: IAccountFactory,
    private val wordsManager: WordsManager,
    private val thirdKeyboardStorage: IThirdKeyboard,
) : ViewModelUiState<UiState>() {

    val mnemonicLanguages = Language.values().toList()

    private var passphraseEnabled: Boolean = false
    private var passphrase: String = ""
    private var passphraseError: String? = null
    private var wordItems: List<RestoreMnemonicModule.WordItem> = listOf()
    private var invalidWordItems: List<RestoreMnemonicModule.WordItem> = listOf()
    private var invalidWordRanges: List<IntRange> = listOf()
    private var error: String? = null
    private var accountType: cash.p.terminal.wallet.AccountType? = null
    private var wordSuggestions: RestoreMnemonicModule.WordSuggestions? = null
    private var language = Language.English
    private var text = ""
    private var cursorPosition = 0
    private var mnemonicWordList = WordList.wordList(language)

    private val regex = Regex("\\S+")

    val defaultName = accountFactory.getNextAccountName()
    var accountName: String = defaultName
        get() = field.ifBlank { defaultName }
        private set

    val isThirdPartyKeyboardAllowed: Boolean
        get() = CoreApp.thirdKeyboardStorage.isThirdPartyKeyboardAllowed

    override fun createState() = UiState(
        passphraseEnabled = passphraseEnabled,
        passphraseError = passphraseError,
        invalidWordRanges = invalidWordRanges,
        error = error,
        accountType = accountType,
        wordSuggestions = wordSuggestions,
        language = language,
    )

    private fun processText() {
        wordItems = wordItems(text)
        invalidWordItems = wordItems.filter { !mnemonicWordList.validWord(it.word, false) }

        val wordItemWithCursor = wordItems.find {
            it.range.contains(cursorPosition - 1)
        }

        val invalidWordItemsExcludingCursoredPartiallyValid = when {
            wordItemWithCursor != null && mnemonicWordList.validWord(wordItemWithCursor.word, true) -> {
                invalidWordItems.filter { it != wordItemWithCursor }
            }
            else -> invalidWordItems
        }

        invalidWordRanges = invalidWordItemsExcludingCursoredPartiallyValid.map { it.range }
        wordSuggestions = wordItemWithCursor?.let {
            RestoreMnemonicModule.WordSuggestions(it, mnemonicWordList.fetchSuggestions(it.word))
        }
    }

    fun onTogglePassphrase(enabled: Boolean) {
        passphraseEnabled = enabled
        passphrase = ""
        passphraseError = null

        emitState()
    }

    fun onEnterName(name: String) {
        accountName = name
    }

    fun onEnterPassphrase(passphrase: String) {
        this.passphrase = passphrase
        passphraseError = null

        emitState()
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
        mnemonicWordList = WordList.wordList(language)
        processText()

        emitState()
    }

    fun onProceed() {
        when {
            invalidWordItems.isNotEmpty() -> {
                invalidWordRanges = invalidWordItems.map { it.range }
            }
            wordItems.size !in (Mnemonic.EntropyStrength.values().map { it.wordCount }) -> {
                error = cash.p.terminal.strings.helpers.Translator.getString(R.string.Restore_Error_MnemonicWordCount, wordItems.size)
            }
            passphraseEnabled && passphrase.isBlank() -> {
                passphraseError = cash.p.terminal.strings.helpers.Translator.getString(R.string.Restore_Error_EmptyPassphrase)
            }
            else -> {
                try {
                    val words = wordItems.map { it.word }
                    wordsManager.validateChecksum(words)

                    accountType = cash.p.terminal.wallet.AccountType.Mnemonic(words, passphrase)
                    error = null
                } catch (checksumException: Exception) {
                    error = cash.p.terminal.strings.helpers.Translator.getString(R.string.Restore_InvalidChecksum)
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

    private fun wordItems(text: String): List<RestoreMnemonicModule.WordItem> {
        return regex.findAll(text.lowercase())
            .map { RestoreMnemonicModule.WordItem(it.value, it.range) }
            .toList()
    }
}
