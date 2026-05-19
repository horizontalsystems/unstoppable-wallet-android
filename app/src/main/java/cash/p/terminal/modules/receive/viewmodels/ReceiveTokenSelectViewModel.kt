package cash.p.terminal.modules.receive.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.eligibleTokens
import cash.p.terminal.core.isDefault
import cash.p.terminal.core.utils.Utils
import cash.p.terminal.modules.receive.FullCoinsProvider
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.WalletFactory
import cash.p.terminal.wallet.entities.FullCoin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.useCases.GetHardwarePublicKeyForWalletUseCase
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class ReceiveTokenSelectViewModel(
    private val walletManager: IWalletManager,
    private val activeAccount: Account,
    private val fullCoinsProvider: FullCoinsProvider
) : ViewModel() {
    private var fullCoins: List<FullCoin> = listOf()

    var uiState by mutableStateOf(
        ReceiveTokenSelectUiState(
            fullCoins = fullCoins,
        )
    )
    private val getHardwarePublicKeyForWalletUseCase: GetHardwarePublicKeyForWalletUseCase by inject(
        GetHardwarePublicKeyForWalletUseCase::class.java
    )
    private val walletFactory: WalletFactory by inject(WalletFactory::class.java)

    init {
        fullCoinsProvider.setActiveWallets(walletManager.activeWallets)

        viewModelScope.launch {
            refreshItems()
            emitState()
        }
    }

    fun updateFilter(q: String) {
        viewModelScope.launch {
            fullCoinsProvider.setQuery(q)
            refreshItems()

            emitState()
        }
    }

    private suspend fun refreshItems() {
        fullCoins = fullCoinsProvider.getItems()
    }


    private fun emitState() {
        viewModelScope.launch {
            uiState = ReceiveTokenSelectUiState(
                fullCoins = fullCoins,
            )
        }
    }

    suspend fun getCoinForReceiveType(fullCoin: FullCoin): CoinForReceiveType? {
        val eligibleTokens = fullCoin.eligibleTokens(activeAccount.type)

        return when {
            eligibleTokens.isEmpty() -> null
            eligibleTokens.size == 1 -> {
                val wallet = getOrCreateWallet(eligibleTokens.first()) ?: return null
                CoinForReceiveType.Single(wallet)
            }

            eligibleTokens.all { it.type is TokenType.Derived } ||
                eligibleTokens.all { it.isLitecoinAddressVariant() } -> {
                receiveType(fullCoin, eligibleTokens, CoinForReceiveType.MultipleDerivations)
            }

            eligibleTokens.all { it.type is TokenType.AddressTyped } -> {
                receiveType(fullCoin, eligibleTokens, CoinForReceiveType.MultipleAddressTypes)
            }

            else -> CoinForReceiveType.MultipleBlockchains
        }
    }

    private suspend fun receiveType(
        fullCoin: FullCoin,
        eligibleTokens: List<Token>,
        multipleType: CoinForReceiveType
    ): CoinForReceiveType? {
        val activeWallets = walletManager.activeWallets.filter { it.coin == fullCoin.coin }

        return when {
            activeWallets.isEmpty() -> {
                eligibleTokens.find { it.type.isDefault }?.let { default ->
                    val wallet = createWallet(default) ?: return null
                    CoinForReceiveType.Single(wallet)
                }
            }

            activeWallets.size == 1 -> {
                CoinForReceiveType.Single(activeWallets.first())
            }

            else -> {
                multipleType
            }
        }
    }

    private fun Token.isLitecoinAddressVariant(): Boolean {
        return blockchainType == BlockchainType.Litecoin &&
            (type is TokenType.Derived || type == TokenType.Mweb)
    }

    private suspend fun getOrCreateWallet(token: Token): Wallet? {
        return walletManager
            .activeWallets
            .find { it.token == token }
            ?: createWallet(token)
    }

    private suspend fun createWallet(token: Token): Wallet? {
        val wallet = walletFactory.create(
            token = token,
            account = activeAccount,
            hardwarePublicKey = getHardwarePublicKeyForWalletUseCase(activeAccount, token)
        ) ?: return null

        walletManager.save(listOf(wallet))

        Utils.waitUntil(1000L, 100L) {
            App.adapterManager.getReceiveAdapterForWallet(wallet) != null
        }

        return wallet
    }

    class Factory(private val activeAccount: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val fullCoinsProvider = FullCoinsProvider(
                App.marketKit,
                activeAccount
            )
            return ReceiveTokenSelectViewModel(
                App.walletManager,
                activeAccount,
                fullCoinsProvider
            ) as T
        }
    }
}

sealed interface CoinForReceiveType {
    data class Single(val wallet: Wallet) : CoinForReceiveType
    object MultipleDerivations : CoinForReceiveType
    object MultipleAddressTypes : CoinForReceiveType
    object MultipleBlockchains : CoinForReceiveType
}

data class ReceiveTokenSelectUiState(
    val fullCoins: List<FullCoin>
)
