package io.horizontalsystems.walletkit.modules.restoreaccount.restoremnemonic

import io.horizontalsystems.walletkit.CoreApp
import io.horizontalsystems.walletkit.IThirdKeyboard
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.managers.WordsManager
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.normalizeNFKD
import io.horizontalsystems.walletkit.modules.restoreaccount.restoremnemonic.RestoreMnemonicModule.UiState
import io.horizontalsystems.walletkit.modules.restoreaccount.restoremnemonic.RestoreMnemonicModule.WordItem
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hdwalletkit.WordList

class RestoreMnemonicViewModel(
    private val accountManager: IAccountManager,
    private val wordsManager: WordsManager,
    private val thirdKeyboardStorage: IThirdKeyboard,
) : ViewModelUiState<UiState>() {

    val mnemonicLanguages = Language.values().toList()

    private var advancedOptionsEnabled: Boolean = false
    private var passphrase: String = ""
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

    private val regex = Regex("\\S+")

    val defaultName = accountManager.getRandomWalletName()
    private var _accountName: String = defaultName
    val accountName: String get() = _accountName.ifBlank { defaultName }

    val isThirdPartyKeyboardAllowed: Boolean
        get() = CoreApp.thirdKeyboardStorage.isThirdPartyKeyboardAllowed

    override fun createState() = UiState(
        accountName = _accountName,
        advancedOptionsEnabled = advancedOptionsEnabled,
        invalidWordRanges = invalidWordRanges,
        error = error,
        accountType = accountType,
        wordSuggestions = wordSuggestions,
        language = language,
    )

    private fun processText() {
        wordItems = wordItems(text)
        // A word may belong to the BIP39 list of the selected language or to Monero's legacy
        // wordlist (the two are disjoint); which seed kind it is gets decided by the final
        // word count in onProceed, so both stay unmarked while typing.
        invalidWordItems = wordItems.filter { !validWord(it.word.normalizeNFKD()) }

        val wordItemWithCursor = wordItems.find {
            it.range.contains(cursorPosition - 1)
        }

        val invalidWordItemsExcludingCursoredPartiallyValid = when {
            wordItemWithCursor != null && validWordPartial(wordItemWithCursor.word.normalizeNFKD()) -> {
                invalidWordItems.filter { it != wordItemWithCursor }
            }
            else -> invalidWordItems
        }

        invalidWordRanges = invalidWordItemsExcludingCursoredPartiallyValid.map { it.range }
        wordSuggestions = wordItemWithCursor?.let { item ->
            val word = item.word.normalizeNFKD()
            val options = mnemonicWordList.fetchSuggestions(word) + ChainRegistry.all.flatMap { it.altMnemonicSuggestions(word) }
            RestoreMnemonicModule.WordSuggestions(item, options.distinct())
        }
    }

    private fun validWord(word: String): Boolean {
        return mnemonicWordList.validWord(word, false) || ChainRegistry.all.any { it.isAltMnemonicWord(word, false) }
    }

    private fun validWordPartial(word: String): Boolean {
        return mnemonicWordList.validWord(word, true) || ChainRegistry.all.any { it.isAltMnemonicWord(word, true) }
    }

    fun onToggleAdvancedOptions(enabled: Boolean) {
        advancedOptionsEnabled = enabled
        if (!enabled) {
            passphrase = ""
            language = Language.English
        }
        emitState()
    }

    fun onEnterPassphrase(passphrase: String) {
        this.passphrase = passphrase
        emitState()
    }

    fun onEnterName(name: String) {
        _accountName = name
    }

    fun generateRandomAccountName() {
        _accountName = accountManager.getRandomWalletName()
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
        mnemonicWordList = WordList.wordListStrict(language)
        processText()

        emitState()
    }

    fun onProceed() {
        when {
            invalidWordItems.isNotEmpty() -> {
                invalidWordRanges = invalidWordItems.map { it.range }
            }
            // 25 words can only be a chain-specific legacy seed (Monero): BIP39 tops out at 24
            ChainRegistry.all.any { it.altMnemonicWordCount == wordItems.size } -> {
                proceedWithAltMnemonic()
            }
            wordItems.size !in (Mnemonic.EntropyStrength.values().map { it.wordCount }) -> {
                error = Translator.getString(R.string.Restore_Error_MnemonicWordCount)
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

    private fun proceedWithAltMnemonic() {
        val words = wordItems.map { it.word.normalizeNFKD() }
        val plugin = ChainRegistry.all.firstOrNull { it.altMnemonicWordCount == words.size } ?: return

        val invalidForChain = wordItems.filter { !plugin.isAltMnemonicWord(it.word.normalizeNFKD(), false) }
        if (invalidForChain.isNotEmpty()) {
            invalidWordRanges = invalidForChain.map { it.range }
            return
        }

        val account = plugin.buildAltMnemonicAccount(words, passphrase.normalizeNFKD())
        if (account != null) {
            accountType = account
            error = null
        } else {
            error = Translator.getString(R.string.Restore_InvalidChecksum)
        }
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