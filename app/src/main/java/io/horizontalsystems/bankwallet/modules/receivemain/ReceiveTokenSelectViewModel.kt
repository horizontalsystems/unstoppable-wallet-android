package cash.p.terminal.modules.receivemain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.eligibleTokens
import cash.p.terminal.core.isDefault
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.nativeTokenQueries
import cash.p.terminal.core.supported
import cash.p.terminal.core.supports
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.Wallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.launch

class ReceiveTokenSelectViewModel(
    private val marketKit: MarketKitWrapper,
    private val walletManager: IWalletManager,
    private val activeAccount: Account
) : ViewModel() {
    private var query: String? = null
    private val predefinedTokens: List<Token>
    private var fullCoins: List<FullCoin> = listOf()

    var uiState by mutableStateOf(
        ReceiveTokenSelectUiState(
            fullCoins = fullCoins,
        )
    )

    init {
        val allowedBlockchainTypes =
            BlockchainType.supported.filter { it.supports(activeAccount.type) }
        val tokenQueries = allowedBlockchainTypes
            .map { it.nativeTokenQueries }
            .flatten()
        val supportedNativeTokens = marketKit.tokens(tokenQueries)
        val activeTokens = walletManager.activeWallets.map { it.token }
        predefinedTokens = activeTokens + supportedNativeTokens

        refreshItems()
        emitState()
    }

    fun updateFilter(q: String) {
        viewModelScope.launch {
            query = q
            refreshItems()

            emitState()
        }
    }

    private fun refreshItems() {
        val tmpQuery = query

        fullCoins = if (tmpQuery.isNullOrBlank()) {
            val coinUids = predefinedTokens.map { it.coin.uid }
            marketKit.fullCoins(coinUids)
        } else {
            marketKit.fullCoins(tmpQuery)
        }
    }


    private fun emitState() {
        viewModelScope.launch {
            uiState = ReceiveTokenSelectUiState(
                fullCoins = fullCoins,
            )
        }
    }

    fun getCoinForReceiveType(fullCoin: FullCoin): CoinForReceiveType? {
        val eligibleTokens = fullCoin.eligibleTokens(activeAccount.type)

        return when {
            eligibleTokens.isEmpty() -> null
            eligibleTokens.size == 1 -> {
                CoinForReceiveType.Single(getOrCreateWallet(eligibleTokens.first()))
            }

            eligibleTokens.all { it.type is TokenType.Derived } -> {
                val activeWallets =
                    walletManager.activeWallets.filter { it.coin == fullCoin.coin }

                when {
                    activeWallets.isEmpty() -> {
                        eligibleTokens.find { it.type.isDefault }?.let { default ->
                            CoinForReceiveType.Single(createWallet(default))
                        }
                    }

                    activeWallets.size == 1 -> {
                        CoinForReceiveType.Single(activeWallets.first())
                    }

                    else -> {
                        CoinForReceiveType.MultipleDerivations
                    }
                }
            }

            eligibleTokens.all { it.type is TokenType.AddressTyped } -> {
                val activeWallets =
                    walletManager.activeWallets.filter { it.coin == fullCoin.coin }

                when {
                    activeWallets.isEmpty() -> {
                        eligibleTokens.find { it.type.isDefault }?.let { default ->
                            CoinForReceiveType.Single(createWallet(default))
                        }
                    }

                    activeWallets.size == 1 -> {
                        CoinForReceiveType.Single(activeWallets.first())
                    }

                    else -> {
                        CoinForReceiveType.MultipleAddressTypes
                    }
                }
            }

            else -> CoinForReceiveType.MultipleBlockchains
        }
    }

    private fun getOrCreateWallet(token: Token): Wallet {
        return walletManager
            .activeWallets
            .find { it.token == token }
            ?: createWallet(token)
    }

    private fun createWallet(token: Token): Wallet {
        val wallet = Wallet(token, activeAccount)

        walletManager.save(listOf(wallet))
        return wallet
    }

    class Factory(private val activeAccount: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiveTokenSelectViewModel(
                App.marketKit,
                App.walletManager,
                activeAccount
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
