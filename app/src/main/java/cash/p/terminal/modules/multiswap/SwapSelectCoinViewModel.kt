package cash.p.terminal.modules.multiswap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.defaultTokenQuery
import cash.p.terminal.core.eligibleTokens
import cash.p.terminal.core.nativeTokenQueries
import cash.p.terminal.core.order
import cash.p.terminal.core.supported
import cash.p.terminal.core.supports
import io.horizontalsystems.core.entities.CurrencyValue
import cash.p.terminal.modules.receive.FullCoinsProvider
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.badge
import cash.p.terminal.wallet.entities.TokenQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class SwapSelectCoinViewModel(
    private val otherSelectedToken: Token?,
    private val activeAccount: Account
) : ViewModel() {
    private val coinsProvider = FullCoinsProvider(App.marketKit, activeAccount)
    private val adapterManager = App.adapterManager
    private val currencyManager = App.currencyManager
    private val marketKit = App.marketKit
    private var query = ""

    private var coinBalanceItems = listOf<CoinBalanceItem>()

    var uiState by mutableStateOf(
        SwapSelectCoinUiState(
            coinBalanceItems = coinBalanceItems,
            loading = true
        )
    )

    init {
        coinsProvider.setActiveWallets(App.walletManager.activeWallets)
        viewModelScope.launch {
            reloadItems()
            emitState(loading = false)
        }
    }

    fun setQuery(q: String) {
        query = q
        coinsProvider.setQuery(q)
        viewModelScope.launch {
            reloadItems()
            emitState()
        }
    }

    private suspend fun reloadItems() = withContext(Dispatchers.IO) {
        val activeWallets = App.walletManager.activeWallets
        val resultTokens = mutableListOf<CoinBalanceItem>()

        if (query.isEmpty()) {
            //Enabled Tokens
            activeWallets.map { wallet ->
                val balance =
                    adapterManager.getBalanceAdapterForWallet(wallet)?.balanceData?.available
                CoinBalanceItem(wallet.token, balance, getFiatValue(wallet.token, balance))
            }.sortedWith(
                if (otherSelectedToken != null) {
                    compareBy<CoinBalanceItem> { it.token.blockchainType != otherSelectedToken.blockchainType }
                        .thenByDescending { it.fiatBalanceValue?.value }
                } else {
                    compareByDescending { it.fiatBalanceValue?.value }
                }
                    .thenBy { it.token.coin.code }
                    .thenBy { it.token.blockchainType.order }
                    .thenBy { it.token.badge }
            )
                .let {
                    resultTokens.addAll(it)
                }

            // Suggested Tokens
            otherSelectedToken?.let { otherToken ->
                val topFullCoins = marketKit.fullCoins("", limit = 100)
                val tokens =
                    topFullCoins.map { fullCoin ->
                        fullCoin.tokens.filter { it.blockchainType == otherToken.blockchainType }
                    }
                        .flatten()
                val suggestedTokens = tokens.filter { tokenToFilter ->
                    tokenToFilter.blockchainType.supports(activeAccount.type) && resultTokens.none { tokenToFilter == it.token }
                }

                suggestedTokens
                    .sortedWith(
                        compareBy<Token> { it.coin.marketCapRank }
                            .thenBy { it.blockchainType.order }
                            .thenBy { it.badge }
                    )
                    .map { CoinBalanceItem(it, null, null) }
                    .let {
                        resultTokens.addAll(it)
                    }
            }

            // Featured Tokens
            val tokenQueries: List<TokenQuery> = when (activeAccount.type) {
                is AccountType.HdExtendedKey -> {
                    BlockchainType.supported.map { it.nativeTokenQueries }.flatten()
                }

                else -> {
                    BlockchainType.supported.map { it.defaultTokenQuery }
                }
            }

            val supportedNativeTokens = marketKit.tokens(tokenQueries)
            supportedNativeTokens.filter { token ->
                token.blockchainType.supports(activeAccount.type) && resultTokens.none { it.token == token }
            }
                .sortedWith(
                    compareBy<Token> { it.blockchainType.order }
                        .thenBy { it.badge }
                ).map {
                    CoinBalanceItem(it, null, null)
                }.let {
                    resultTokens.addAll(it)
                }

            coinBalanceItems = resultTokens
            return@withContext
        }

        coinBalanceItems = coinsProvider.getItems()
            .map { it.eligibleTokens(activeAccount.type) }
            .flatten()
            .map {
                val balance: BigDecimal? =
                    activeWallets.firstOrNull { wallet ->
                        wallet.coin.uid == it.coin.uid &&
                                wallet.token.blockchainType == it.blockchainType &&
                                wallet.token.type == it.type
                    }
                        ?.let { wallet ->
                            adapterManager.getBalanceAdapterForWallet(wallet)?.balanceData?.available
                        }
                CoinBalanceItem(it, balance, getFiatValue(it, balance))
            }
            .sortedWith(compareByDescending { it.balance })
    }

    private fun emitState(loading: Boolean = false) {
        uiState = SwapSelectCoinUiState(
            coinBalanceItems = coinBalanceItems,
            loading = loading
        )
    }

    private fun getFiatValue(token: Token, balance: BigDecimal?): CurrencyValue? {
        return balance?.let {
            getXRate(token)?.multiply(it)
        }?.let { fiatBalance ->
            CurrencyValue(currencyManager.baseCurrency, fiatBalance)
        }
    }

    private fun getXRate(token: Token): BigDecimal? {
        val currency = currencyManager.baseCurrency
        return marketKit.coinPrice(token.coin.uid, currency.code)?.value
    }

}

data class SwapSelectCoinUiState(
    val coinBalanceItems: List<CoinBalanceItem>,
    val loading: Boolean = true
)
