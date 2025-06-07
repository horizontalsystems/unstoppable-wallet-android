package cash.p.terminal.modules.hardwarewallet

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import cash.p.terminal.core.App
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.managers.WalletActivator
import cash.p.terminal.core.storage.AppDatabase
import cash.p.terminal.core.toHexString
import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.tangem.domain.usecase.BuildHardwarePublicKeyUseCase
import cash.p.terminal.tangem.domain.usecase.TangemScanUseCase
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.BuildConfig
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IHardwarePublicKeyStorage
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HardwareWalletViewModel(
    private val accountManager: IAccountManager,
    private val tangemScanUseCase: TangemScanUseCase,
    private val hardwarePublicKeyStorage: IHardwarePublicKeyStorage,
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val accountFactory: IAccountFactory = App.accountFactory
    private val walletActivator: WalletActivator = App.walletActivator

    val defaultAccountName = accountFactory.getNextHardwareAccountName()
    var accountName: String = defaultAccountName
        get() = field.ifBlank { defaultAccountName }
        private set

    var success by mutableStateOf<AccountType?>(null)
        private set

    private val _errorEvents = Channel<HardwareWalletError>(capacity = 1)
    val errorEvents = _errorEvents.receiveAsFlow()

    fun scanCard() = viewModelScope.launch {
        tangemScanUseCase.scanProduct(getDefaultTokens())
            .doOnSuccess { scanResponse ->
                if (scanResponse.card.isAccessCodeSet && scanResponse.card.wallets.isNotEmpty()) {
                    // Card already activated, need to add tokens
                    createHardwareCardAccount(scanResponse)
                } else {
                    _errorEvents.trySend(HardwareWalletError.CardNotActivated)
                }
            }
            .doOnFailure {
                Log.d("CreateAccountViewModel", "Error scanning card: ${it.customMessage}")
            }
    }

    private suspend fun createHardwareCardAccount(scanResponse: ScanResponse) {
        val accountType = AccountType.HardwareCard(
            cardId = scanResponse.card.cardId,
            walletPublicKey = scanResponse.card.cardPublicKey.toHexString()
        )
        val account = accountFactory.account(
            name = accountName,
            type = accountType,
            origin = AccountOrigin.Created,
            backedUp = false,
            fileBackedUp = false,
        )

        val defaultTokens = getDefaultTokens()

        val blockchainTypes = defaultTokens.distinct()
        val publicKeys =
            BuildHardwarePublicKeyUseCase().invoke(scanResponse, account.id, blockchainTypes)
        appDatabase.withTransaction {
            hardwarePublicKeyStorage.save(publicKeys)
            activateDefaultWallets(
                account = account,
                tokenQueries = defaultTokens.filter { defaultToken ->
                    publicKeys.find { it.blockchainType == defaultToken.blockchainType.uid } != null
                }
            )
            accountManager.save(account = account, updateActive = false)
        }

        accountManager.setActiveAccountId(account.id)
        success = accountType
    }

    fun onChangeAccountName(name: String) {
        accountName = name
    }

    fun onSuccessMessageShown() {
        success = null
    }

    private suspend fun activateDefaultWallets(
        account: Account,
        tokenQueries: List<TokenQuery> = getDefaultTokens()
    ) = walletActivator.activateWalletsSuspended(account, tokenQueries)

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
}
