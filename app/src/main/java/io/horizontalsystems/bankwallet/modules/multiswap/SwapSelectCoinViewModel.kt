package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.defaultTokenQuery
import io.horizontalsystems.bankwallet.core.eligibleTokens
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.core.sorting.SortCriterion
import io.horizontalsystems.bankwallet.core.sorting.TokenSortContext
import io.horizontalsystems.bankwallet.core.supported
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceSorter
import io.horizontalsystems.bankwallet.modules.receive.FullCoinsProvider
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class SwapSelectCoinViewModel(private val otherSelectedToken: Token?) : ViewModel() {
    private val activeAccount = App.accountManager.activeAccount
    private val coinsProvider = activeAccount?.let { FullCoinsProvider(App.marketKit, it) }
    private val adapterManager = App.adapterManager
    private val currencyManager = App.currencyManager
    private val marketKit = App.marketKit
    private val localStorage = App.localStorage
    private var query = ""

    private var popular = listOf<CoinBalanceItem>()
    private var yourTokens = listOf<CoinBalanceItem>()
    private var topTokens = listOf<CoinBalanceItem>()
    private var searchResults = listOf<CoinBalanceItem>()
    private var recent = listOf<CoinBalanceItem>()

    var uiState by mutableStateOf(
        SwapSelectCoinUiState(
            query = query,
            popular = popular,
            yourTokens = yourTokens,
            topTokens = topTokens,
            searchResults = searchResults,
            recent = recent,
        )
    )
        private set

    init {
        coinsProvider?.setActiveWallets(App.walletManager.activeWallets)
        viewModelScope.launch {
            loadSections()
            emitState()
        }
    }

    fun setQuery(q: String) {
        query = q
        coinsProvider?.setQuery(q)
        viewModelScope.launch {
            searchResults = if (q.isBlank()) emptyList() else search(q)
            emitState()
        }
    }

    /**
     * Records a token that the user picked while the search field was active, so it can be shown
     * in the Recent section. Most recent first, deduplicated, capped at [RECENT_LIMIT].
     */
    fun onRecentTokenSelected(token: Token) {
        val id = token.tokenQuery.id
        val ids = (listOf(id) + localStorage.swapRecentTokenQueryIds.filter { it != id })
            .take(RECENT_LIMIT)
        localStorage.swapRecentTokenQueryIds = ids

        viewModelScope.launch {
            recent = loadRecent(ids)
            emitState()
        }
    }

    private suspend fun loadRecent(ids: List<String>): List<CoinBalanceItem> =
        withContext(Dispatchers.Default) {
            val activeWallets = App.walletManager.activeWallets
            ids.mapNotNull { id -> TokenQuery.fromId(id)?.let { marketKit.token(it) } }
                .map { coinBalanceItem(it, activeWallets) }
        }

    private fun coinBalanceItem(token: Token, activeWallets: List<Wallet>): CoinBalanceItem {
        val balance = activeWallets.firstOrNull { it.token == token }?.let {
            adapterManager.getBalanceAdapterForWallet(it)?.balanceData?.available
        }
        return CoinBalanceItem(token, balance, getFiatValue(token, balance))
    }

    private suspend fun loadSections() = withContext(Dispatchers.Default) {
        val activeWallets = App.walletManager.activeWallets

        // Recent — tokens the user picked previously while searching
        recent = loadRecent(localStorage.swapRecentTokenQueryIds)

        // Your Tokens — all enabled tokens, sorted as on the main Wallet screen
        yourTokens = activeWallets
            .map { coinBalanceItem(it.token, activeWallets) }
            .sortedByCriteria(BalanceSorter.VALUE_CRITERIA)

        // Popular Tokens — context-aware hardcoded list (built from the opposite token)
        popular = buildPopularTokens(otherSelectedToken).map { CoinBalanceItem(it, null, null) }

        // Top Tokens — top 25 by market cap, excluding everything in Popular and Your Tokens
        val excludedIds = (popular + yourTokens).map { it.token.tokenQuery.id }.toMutableSet()
        val top = mutableListOf<CoinBalanceItem>()
        val topCoins = marketKit.fullCoins("", 100)
            .sortedBy { it.coin.marketCapRank ?: Int.MAX_VALUE }
        for (fullCoin in topCoins) {
            if (top.size >= 25) break

            val eligible = if (activeAccount != null) {
                fullCoin.eligibleTokens(activeAccount.type)
            } else {
                fullCoin.tokens.filter { it.blockchainType in BlockchainType.supported }
            }
            val representative = eligible
                .map { CoinBalanceItem(it, null, null) }
                .sortedByCriteria(
                    listOf(SortCriterion.CodeNativeFirst, SortCriterion.BlockchainOrder, SortCriterion.Badge)
                )
                .firstOrNull { it.token.tokenQuery.id !in excludedIds }
                ?: continue

            top.add(representative)
            excludedIds.add(representative.token.tokenQuery.id)
        }
        topTokens = top
    }

    /**
     * Popular tokens depend on the opposite (context) token. The list is assembled from a fixed
     * formula, then cleaned up: nulls and the context token are dropped and duplicates removed
     * (first occurrence kept). See token_picker spec for the exact rules.
     */
    private fun buildPopularTokens(context: Token?): List<Token> {
        val baseNativeTypes = listOf(
            BlockchainType.Bitcoin,
            BlockchainType.Ethereum,
            BlockchainType.Monero,
            BlockchainType.Zcash,
            BlockchainType.Tron,
        )
        val natives = marketKit.tokens(baseNativeTypes.map { it.defaultTokenQuery })
            .associateBy { it.blockchainType }

        val stableCoins = marketKit.fullCoins(listOf("tether", "usd-coin"))
        val usdt = stableCoins.firstOrNull { it.coin.uid == "tether" }?.tokens
            ?.groupBy { it.blockchainType }?.mapValues { it.value.first() } ?: emptyMap()
        val usdc = stableCoins.firstOrNull { it.coin.uid == "usd-coin" }?.tokens
            ?.groupBy { it.blockchainType }?.mapValues { it.value.first() } ?: emptyMap()

        val usdtEth = usdt[BlockchainType.Ethereum]
        val usdcEth = usdc[BlockchainType.Ethereum]

        val baseNatives = baseNativeTypes.map { natives[it] }
        val tailStables = listOf(
            usdtEth,
            usdt[BlockchainType.Tron],
            usdt[BlockchainType.BinanceSmartChain],
            usdt[BlockchainType.Base],
        )

        val ordered: List<Token?> = when {
            context == null ->
                baseNatives + listOf(usdtEth, usdcEth) + tailStables

            context.type.isNative -> {
                // Case B — context is a native coin
                val usdtSame = usdt[context.blockchainType] ?: usdtEth
                val usdcSame = usdc[context.blockchainType] ?: usdcEth
                listOf(usdtSame, usdcSame) + tailStables + baseNatives
            }

            else -> {
                // Case A — context is a stablecoin or any other non-native token
                val nativeSame = marketKit.token(context.blockchainType.defaultTokenQuery)
                val usdtSame = usdt[context.blockchainType] ?: usdtEth
                val usdcSame = usdc[context.blockchainType] ?: usdcEth
                listOf(nativeSame) + baseNatives + listOf(usdtSame, usdcSame) + tailStables
            }
        }

        val seenIds = mutableSetOf<String>()
        val result = mutableListOf<Token>()
        for (token in ordered) {
            if (token == null) continue
            // can't swap into the context token itself
            if (context != null &&
                token.coin.uid == context.coin.uid &&
                token.blockchainType == context.blockchainType
            ) continue
            if (!seenIds.add(token.tokenQuery.id)) continue
            result.add(token)
        }
        return result
    }

    private suspend fun search(q: String): List<CoinBalanceItem> = withContext(Dispatchers.Default) {
        val activeWallets = App.walletManager.activeWallets

        if (coinsProvider != null && activeAccount != null) {
            coinsProvider.getItems()
                .map { it.eligibleTokens(activeAccount.type) }
                .flatten()
                .map { token ->
                    val wallet = activeWallets.firstOrNull { it.token == token }
                    val balance = wallet?.let {
                        adapterManager.getBalanceAdapterForWallet(it)?.balanceData?.available
                    }
                    CoinBalanceItem(token, balance, getFiatValue(token, balance))
                }
                .sortedByCriteria(
                    listOf(
                        SortCriterion.Enabled,
                        SortCriterion.FilterRelevance,
                        SortCriterion.CodeNativeFirst,
                        SortCriterion.BlockchainOrder,
                        SortCriterion.Badge
                    ),
                    TokenSortContext(filter = q, enabledTokens = activeWallets.map { it.token }.toSet())
                )
        } else {
            marketKit.fullCoins(q, 100)
                .flatMap { fullCoin -> fullCoin.tokens }
                .filter { it.blockchainType in BlockchainType.supported }
                .map { token -> CoinBalanceItem(token, null, null) }
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SwapSelectCoinUiState(
                query = query,
                popular = popular,
                yourTokens = yourTokens,
                topTokens = topTokens,
                searchResults = searchResults,
                recent = recent,
            )
        }
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
        return marketKit.coinPrice(token.coin.uid, currency.code)?.let {
            if (it.expired) {
                null
            } else {
                it.value
            }
        }
    }

    class Factory(private val otherSelectedToken: Token?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapSelectCoinViewModel(otherSelectedToken) as T
        }
    }

    companion object {
        private const val RECENT_LIMIT = 10
    }
}

data class SwapSelectCoinUiState(
    val query: String,
    val popular: List<CoinBalanceItem>,
    val yourTokens: List<CoinBalanceItem>,
    val topTokens: List<CoinBalanceItem>,
    val searchResults: List<CoinBalanceItem>,
    val recent: List<CoinBalanceItem>,
)
