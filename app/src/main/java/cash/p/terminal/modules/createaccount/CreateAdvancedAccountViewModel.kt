package cash.p.terminal.modules.createaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.managers.WalletActivator
import cash.p.terminal.core.managers.WordsManager
import cash.p.terminal.core.providers.PredefinedBlockchainSettingsProvider
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.createaccount.CreateAccountModule.Kind.Mnemonic12
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.BuildConfig
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.PassphraseValidator
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.normalizeNFKD
import io.horizontalsystems.core.entities.BlockchainType

class CreateAdvancedAccountViewModel(
    private val accountFactory: IAccountFactory,
    private val wordsManager: WordsManager,
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator,
    private val passphraseValidator: PassphraseValidator,
    private val predefinedBlockchainSettingsProvider: PredefinedBlockchainSettingsProvider
) : ViewModel() {

    private var passphrase = ""
    private var passphraseConfirmation = ""

    val mnemonicKinds = CreateAccountModule.Kind.values().toList()

    val defaultAccountName = accountFactory.getNextAccountName()
    var accountName: String = defaultAccountName
        get() = field.ifBlank { defaultAccountName }
        private set

    var selectedKind: CreateAccountModule.Kind = Mnemonic12
        private set

    var passphraseEnabled by mutableStateOf(false)
        private set

    var passphraseConfirmState by mutableStateOf<DataState.Error?>(null)
        private set

    var passphraseState by mutableStateOf<DataState.Error?>(null)
        private set

    var success by mutableStateOf<AccountType?>(null)
        private set

    fun createMnemonicAccount() {
        if (passphraseEnabled && passphraseIsInvalid()) {
            return
        }

        val accountType = mnemonicAccountType(selectedKind.wordsCount)
        val account = accountFactory.account(
            name = accountName,
            type = accountType,
            origin = AccountOrigin.Created,
            backedUp = false,
            fileBackedUp = false,
        )

        accountManager.save(account)
        activateDefaultWallets(account)
        predefinedBlockchainSettingsProvider.prepareNew(account, BlockchainType.Zcash)
        success = accountType
    }

    fun onChangeAccountName(name: String) {
        accountName = name
    }

    fun onChangePassphrase(v: String) {
        if (passphraseValidator.containsValidCharacters(v)) {
            passphraseState = null
            passphrase = v
        } else {
            passphraseState = DataState.Error(
                Exception(
                    Translator.getString(R.string.CreateWallet_Error_PassphraseForbiddenSymbols)
                )
            )
        }
    }

    fun onChangePassphraseConfirmation(v: String) {
        passphraseConfirmState = null
        passphraseConfirmation = v
    }

    fun setMnemonicKind(kind: CreateAccountModule.Kind) {
        selectedKind = kind
    }

    fun setPassphraseEnabledState(enabled: Boolean) {
        passphraseEnabled = enabled
        if (!enabled) {
            passphrase = ""
            passphraseConfirmation = ""
        }
    }

    fun onSuccessMessageShown() {
        success = null
    }

    private fun passphraseIsInvalid(): Boolean {
        if (passphraseState is DataState.Error) {
            return true
        }

        if (passphrase.isBlank()) {
            passphraseState = DataState.Error(
                Exception(
                    Translator.getString(R.string.CreateWallet_Error_EmptyPassphrase)
                )
            )
            return true
        }
        if (passphrase != passphraseConfirmation) {
            passphraseConfirmState = DataState.Error(
                Exception(
                    Translator.getString(R.string.CreateWallet_Error_InvalidConfirmation)
                )
            )
            return true
        }
        return false
    }

    private fun activateDefaultWallets(
        account: Account,
        tokenQueries: List<TokenQuery> = getDefaultTokens()
    ) = walletActivator.activateWallets(account, tokenQueries)

    private fun getDefaultTokens() = listOfNotNull(
        TokenQuery(BlockchainType.Bitcoin, TokenType.Derived(TokenType.Derivation.Bip84)),
        TokenQuery(BlockchainType.Ethereum, TokenType.Native),
        TokenQuery(BlockchainType.BinanceSmartChain, TokenType.Native),
        //TokenQuery(BlockchainType.Ethereum, TokenType.Eip20("0xdac17f958d2ee523a2206206994597c13d831ec7")),
        TokenQuery(
            BlockchainType.BinanceSmartChain,
            TokenType.Eip20(BuildConfig.PIRATE_CONTRACT)
        ),
        TokenQuery(
            BlockchainType.BinanceSmartChain,
            TokenType.Eip20(BuildConfig.COSANTA_CONTRACT)
        ),
        //TokenQuery(BlockchainType.BinanceSmartChain, TokenType.Eip20("0xe9e7cea3dedca5984780bafc599bd69add087d56")),
    )

    private fun mnemonicAccountType(wordCount: Int): AccountType {
        // A new account can be created only using an English wordlist and limited chars in the passphrase.
        // Despite it, we add text normalizing.
        // It is to avoid potential issues if we allow non-English wordlists on account creation.
        val words = wordsManager.generateWords(wordCount).map { it.normalizeNFKD() }
        return AccountType.Mnemonic(words, passphrase.normalizeNFKD())
    }
}
