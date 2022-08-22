package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.core.providers.PredefinedBlockchainSettingsProvider
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.createaccount.CreateAccountModule.Kind.Mnemonic12
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class CreateAccountViewModel(
    private val accountFactory: IAccountFactory,
    private val wordsManager: WordsManager,
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator,
    private val predefinedBlockchainSettingsProvider: PredefinedBlockchainSettingsProvider,
) : ViewModel() {

    private var passphrase = ""
    private var passphraseConfirmation = ""

    val mnemonicKinds = CreateAccountModule.Kind.values().toList()
    val mnemonicLanguages = Language.values().toList()

    var selectedKind: CreateAccountModule.Kind = Mnemonic12
        private set

    var selectedLanguage: Language = Language.English
        private set

    var passphraseEnabled by mutableStateOf(false)
        private set

    var passphraseConfirmState by mutableStateOf<DataState.Error?>(null)
        private set

    var passphraseState by mutableStateOf<DataState.Error?>(null)
        private set

    var successMessage by mutableStateOf<Int?>(null)
        private set

    fun createAccount() {
        if (passphraseEnabled && passphraseIsInvalid()) {
            return
        }

        val accountType = mnemonicAccountType(selectedKind.wordsCount)
        val account = accountFactory.account(
            accountFactory.getNextAccountName(),
            accountType,
            AccountOrigin.Created,
            false
        )

        accountManager.save(account)
        activateDefaultWallets(account)
        predefinedBlockchainSettingsProvider.prepareNew(account, BlockchainType.Zcash)
        successMessage = R.string.Hud_Text_Created
    }

    fun onChangePassphrase(v: String) {
        passphraseState = null
        passphrase = v
    }

    fun onChangePassphraseConfirmation(v: String) {
        passphraseConfirmState = null
        passphraseConfirmation = v
    }

    fun setMnemonicKind(kind: CreateAccountModule.Kind) {
        selectedKind = kind
    }

    fun setMnemonicLanguage(language: Language) {
        selectedLanguage = language
    }

    fun setPassphraseEnabledState(enabled: Boolean) {
        passphraseEnabled = enabled
        if (!enabled) {
            passphrase = ""
            passphraseConfirmation = ""
        }
    }

    fun onSuccessMessageShown() {
        successMessage = null
    }

    private fun passphraseIsInvalid(): Boolean {
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

    private fun activateDefaultWallets(account: Account) {
        val tokenQueries = listOf(
            TokenQuery(BlockchainType.Bitcoin, TokenType.Native),
            TokenQuery(BlockchainType.Ethereum, TokenType.Native),
            TokenQuery(BlockchainType.BinanceSmartChain, TokenType.Native),
            TokenQuery(BlockchainType.Avalanche, TokenType.Native),
        )
        walletActivator.activateWallets(account, tokenQueries)
    }

    private fun mnemonicAccountType(wordCount: Int): AccountType {
        val words = wordsManager.generateWords(wordCount, selectedLanguage)
        return AccountType.Mnemonic(words, passphrase)
    }

}
