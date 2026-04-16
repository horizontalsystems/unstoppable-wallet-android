package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.core.providers.PredefinedBlockchainSettingsProvider
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.normalizeNFKD
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.launch

class CreateAccountPasskeyViewModel(
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator,
    private val predefinedBlockchainSettingsProvider: PredefinedBlockchainSettingsProvider,
) : ViewModelUiState<CreateAccountPasskeyUiState>() {

    private val defaultAccountName = accountFactory.getNextAccountName()
    private var accountName: String = defaultAccountName
    private var success: AccountType? = null
    private var error: String? = null

    override fun createState() = CreateAccountPasskeyUiState(
        defaultAccountName = defaultAccountName,
        accountName = accountName,
        success = success,
        error = error,
    )

    /**
     * Called by the Fragment after PasskeyManager.register() succeeds.
     * Derives mnemonic from [entropy], creates and persists the account.
     */
    fun createAccount(entropy: ByteArray) {
        viewModelScope.launch {
            try {
                val words = Mnemonic().toMnemonic(entropy, Language.English)
                    .map { it.normalizeNFKD() }
                val accountType = AccountType.Mnemonic(words, "")
                val account = accountFactory.account(
                    name = accountName,
                    type = accountType,
                    origin = AccountOrigin.Created,
                    backedUp = true,
                    fileBackedUp = false,
                )
                accountManager.save(account)
                activateDefaultWallets(account)
                predefinedBlockchainSettingsProvider.prepareNew(account, BlockchainType.Zcash)
                predefinedBlockchainSettingsProvider.prepareNew(account, BlockchainType.Monero)
                success = accountType
                error = null
            } catch (e: Exception) {
                error = e.message
                success = null
            }
            emitState()
        }
    }

    fun onChangeAccountName(v: String) {
        accountName = v.ifBlank { defaultAccountName }
        emitState()
    }

    /** Called by the Fragment when PasskeyManager.register() throws. */
    fun onError(e: Throwable) {
        error = e.message ?: e.javaClass.simpleName
        success = null
        emitState()
    }

    fun onErrorDisplayed() {
        error = null
        emitState()
    }

    private fun activateDefaultWallets(account: Account) {
        val tokenQueries = listOf(
            TokenQuery(BlockchainType.Bitcoin, TokenType.Derived(TokenType.Derivation.Bip84)),
            TokenQuery(BlockchainType.Ethereum, TokenType.Native),
            TokenQuery(BlockchainType.Monero, TokenType.Native),
            TokenQuery(BlockchainType.Tron, TokenType.Native),
            TokenQuery(BlockchainType.BinanceSmartChain, TokenType.Native),
            TokenQuery(BlockchainType.Tron, TokenType.Eip20("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t")),
            TokenQuery(BlockchainType.Ethereum, TokenType.Eip20("0xdac17f958d2ee523a2206206994597c13d831ec7")),
        )
        walletActivator.activateWallets(account, tokenQueries)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateAccountPasskeyViewModel(
                App.accountFactory,
                App.accountManager,
                App.walletActivator,
                PredefinedBlockchainSettingsProvider(
                    App.restoreSettingsManager,
                    App.zcashBirthdayProvider,
                    App.moneroBirthdayProvider
                )
            ) as T
        }
    }
}

data class CreateAccountPasskeyUiState(
    val defaultAccountName: String,
    val accountName: String,
    val success: AccountType? = null,
    val error: String? = null,
)
