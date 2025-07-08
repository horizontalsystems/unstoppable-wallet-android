package cash.p.terminal.modules.restoreaccount.restoremnemonic

import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.managers.WalletActivator
import cash.p.terminal.core.usecase.MoneroWalletUseCase
import cash.p.terminal.core.usecase.ValidateMoneroHeightUseCase
import cash.p.terminal.core.usecase.ValidateMoneroMnemonicUseCase
import cash.p.terminal.core.utils.MoneroConfig
import cash.p.terminal.modules.restoreaccount.restoremnemonic.RestoreMnemonicModule.UiState
import cash.p.terminal.modules.restoreaccount.restoremnemonic.RestoreMnemonicModule.WordItem
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.normalizeNFKD
import com.m2049r.xmrwallet.util.ledger.Monero
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.IThirdKeyboard
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hdwalletkit.MnemonicWordList
import io.horizontalsystems.hdwalletkit.WordList
import kotlinx.coroutines.launch

class RestoreMnemonicViewModel(
    private val validateMoneroMnemonicUseCase: ValidateMoneroMnemonicUseCase,
    private val validateMoneroHeightUseCase: ValidateMoneroHeightUseCase,
    private val moneroWalletUseCase: MoneroWalletUseCase,
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator
) : ViewModelUiState<UiState>() {

    private val accountFactory: IAccountFactory = App.accountFactory
    private val thirdKeyboardStorage: IThirdKeyboard = App.thirdKeyboardStorage

    val mnemonicLanguages = Language.values().toList()

    private var passphraseEnabled: Boolean = false
    private var passphrase: String = ""
    private var passphraseError: String? = null
    private var wordItems: List<WordItem> = listOf()
    private var invalidWordItems: List<WordItem> = listOf()
    private var invalidWordRanges: List<IntRange> = listOf()
    private var error: String? = null
    private var errorHeight: String? = null
    private var isMoneroMnemonic: Boolean = false
    private var height: String = ""
    private var accountType: AccountType? = null
    private var wordSuggestions: RestoreMnemonicModule.WordSuggestions? = null
    private var language = Language.English
    private var text = ""
    private var cursorPosition = 0
    private var normalMnemonicWordList = WordList.wordListStrict(language)
    private var mnemonicMoneroWordList = MnemonicWordList(Monero.ENGLISH_WORDS.toList(), false)

    private val mnemonicWordList: MnemonicWordList
        get() = if (isMoneroMnemonic) mnemonicMoneroWordList else normalMnemonicWordList


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
        errorHeight = errorHeight,
        height = height,
        isMoneroMnemonic = isMoneroMnemonic,
        accountType = accountType,
        wordSuggestions = wordSuggestions,
        language = language,
    )

    fun onToggleMoneroMnemonic(enabled: Boolean) {
        isMoneroMnemonic = enabled
        processText()
        emitState()
    }

    private fun processText() {
        wordItems = wordItems(text)
        invalidWordItems =
            wordItems.filter { !mnemonicWordList.validWord(it.word.normalizeNFKD(), false) }

        val wordItemWithCursor = wordItems.find {
            it.range.contains(cursorPosition - 1)
        }

        val invalidWordItemsExcludingCursoredPartiallyValid = when {
            wordItemWithCursor != null && mnemonicWordList.validWord(
                wordItemWithCursor.word.normalizeNFKD(),
                true
            ) -> {
                invalidWordItems.filter { it != wordItemWithCursor }
            }

            else -> invalidWordItems
        }

        invalidWordRanges = invalidWordItemsExcludingCursoredPartiallyValid.map { it.range }
        wordSuggestions = wordItemWithCursor?.let {
            RestoreMnemonicModule.WordSuggestions(
                it,
                mnemonicWordList.fetchSuggestions(it.word.normalizeNFKD())
            )
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

    fun onChangeHeightText(text: String) {
        error = null
        this.height = text
//        processText()

        emitState()
    }

    fun setMnemonicLanguage(language: Language) {
        this.language = language
        normalMnemonicWordList = WordList.wordListStrict(language)
        processText()

        emitState()
    }

    fun onProceed() = viewModelScope.launch {
        when {
            invalidWordItems.isNotEmpty() -> {
                invalidWordRanges = invalidWordItems.map { it.range }
            }

            isMoneroMnemonic && wordItems.size != MoneroConfig.WORD_COUNT -> {
                error = Translator.getString(
                    R.string.Restore_Error_MnemonicWordCount_monero,
                    wordItems.size
                )
            }

            isMoneroMnemonic && validateMoneroHeightUseCase(height) == -1L -> {
                errorHeight = Translator.getString(R.string.inavlid_height)
            }

            (!isMoneroMnemonic && wordItems.size !in (Mnemonic.EntropyStrength.entries.map { it.wordCount })) -> {
                error =
                    Translator.getString(R.string.Restore_Error_MnemonicWordCount, wordItems.size)
            }

            passphraseEnabled && passphrase.isBlank() -> {
                passphraseError = Translator.getString(R.string.Restore_Error_EmptyPassphrase)
            }

            else -> {
                try {
                    val words = wordItems.map { it.word.normalizeNFKD() }
                    validateMoneroMnemonicUseCase(words, isMoneroMnemonic)

                    accountType = if (isMoneroMnemonic) {
                        moneroWalletUseCase.restore(
                            words = words,
                            height = validateMoneroHeightUseCase(height)
                        )
                    } else {
                        AccountType.Mnemonic(words, passphrase.normalizeNFKD())
                    }
                    error = if (accountType == null) {
                        Translator.getString(R.string.monero_restore_error)
                    } else {
                        null
                    }
                    errorHeight = null

                    if (accountType is AccountType.MnemonicMonero) {
                        finishRestoringMoneroAccount()
                    }
                } catch (_: Exception) {
                    error = Translator.getString(R.string.Restore_InvalidChecksum)
                }
            }
        }

        emitState()
    }

    private suspend fun finishRestoringMoneroAccount() {
        val accountType = accountType ?: return

        val account = accountFactory.account(
            name = accountName,
            type = accountType,
            origin = AccountOrigin.Restored,
            backedUp = true,
            fileBackedUp = false,
        )

        accountManager.save(account)
        walletActivator.activateWalletsSuspended(
            account,
            listOf(TokenQuery(BlockchainType.Monero, TokenType.Native))
        )
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
