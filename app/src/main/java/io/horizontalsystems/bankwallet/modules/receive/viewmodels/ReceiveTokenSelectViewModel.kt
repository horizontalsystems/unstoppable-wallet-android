package io.horizontalsystems.bankwallet.modules.receive.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.eligibleTokens
import io.horizontalsystems.bankwallet.core.isDefault
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.MoneroBirthdayProvider
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.modules.receive.FullCoinsProvider
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ReceiveTokenSelectViewModel(
    private val walletManager: IWalletManager,
    private val activeAccount: Account,
    private val fullCoinsProvider: FullCoinsProvider,
    private val adapterManager: IAdapterManager,
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
    private val zcashBirthdayProvider: ZcashBirthdayProvider,
    private val moneroBirthdayProvider: MoneroBirthdayProvider,
    private val restoreSettingsManager: RestoreSettingsManager
) : ViewModel() {
    private var fullCoins: List<FullCoin> = listOf()
    private var searchQuery = ""

    var uiState by mutableStateOf(
        ReceiveTokenSelectUiState(
            fullCoins = fullCoins,
            searchQuery = searchQuery,
        )
    )

    init {
        fullCoinsProvider.setActiveWallets(walletManager.activeWallets)

        refreshItems()
        emitState()
    }

    fun updateFilter(q: String) {
        searchQuery = q
        viewModelScope.launch {
            fullCoinsProvider.setQuery(q)
            refreshItems()

            emitState()
        }
    }

    private fun refreshItems() {
        val coins = fullCoinsProvider.getItems()

        if (searchQuery.isEmpty()) {
            val sortableItems = coins.map { fullCoin ->
                val eligibleTokens = fullCoin.eligibleTokens(activeAccount.type)

                val totalFiatValue = eligibleTokens
                    .mapNotNull { token -> walletManager.activeWallets.firstOrNull { it.token == token } }
                    .map { wallet ->
                        val balance =
                            adapterManager.getBalanceAdapterForWallet(wallet)?.balanceData?.available
                                ?: BigDecimal.ZERO
                        getFiatValue(wallet.token, balance)?.value ?: BigDecimal.ZERO
                    }
                    .fold(BigDecimal.ZERO) { acc, value -> acc + value }

                val secondarySortOrder =
                    eligibleTokens.firstOrNull()?.blockchainType?.order ?: Int.MAX_VALUE

                Triple(fullCoin, totalFiatValue, secondarySortOrder)
            }

            val sortedCoins = sortableItems.sortedWith(
                compareByDescending<Triple<FullCoin, BigDecimal, Int>> { it.second } // Primary sort: by total fiat value
                    .thenBy { it.third }
            ).map { it.first }

            fullCoins = sortedCoins
        } else {
            fullCoins = coins
        }
    }


    private fun emitState() {
        viewModelScope.launch {
            uiState = ReceiveTokenSelectUiState(
                fullCoins = fullCoins,
                searchQuery = searchQuery,
            )
        }
    }

    fun shouldShowBottomSheet(fullCoin: FullCoin): Boolean {
        val token = fullCoin.tokens.firstOrNull() ?: return false

        if (token.blockchainType == BlockchainType.Zcash || token.blockchainType == BlockchainType.Monero) {
            fullCoin.tokens.firstOrNull()?.let {
                val activeWallets =
                    walletManager.activeWallets.filter { it.coin == fullCoin.coin }
                if (activeWallets.isEmpty()) {
                    return true
                }
            }
        }
        return false
    }

    suspend fun getCoinForReceiveType(fullCoin: FullCoin): CoinForReceiveType? {
        val eligibleTokens = fullCoin.eligibleTokens(activeAccount.type)

        return when {
            eligibleTokens.isEmpty() -> null
            eligibleTokens.size == 1 -> {
                val wallet = getOrCreateWallet(eligibleTokens.first())
                if (eligibleTokens.first().blockchainType == BlockchainType.Zcash) {
                    CoinForReceiveType.MultipleZcashAddressTypes(wallet)
                } else {
                    CoinForReceiveType.Single(wallet)
                }
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

    suspend fun getWalletForCoinWithBirthday(
        coin: FullCoin,
        config: BirthdayHeightConfig
    ): Wallet? {
        val token = coin.tokens.firstOrNull() ?: return null

        val birthdayHeight = if (config.restoreAsNew) {
            getBirthdayHeightForNewWallet(token.blockchainType)
        } else {
            config.birthdayHeight?.toLongOrNull()
        }

        if (birthdayHeight != null) {
            val settings = RestoreSettings().apply {
                this.birthdayHeight = birthdayHeight
            }
            restoreSettingsManager.save(settings, activeAccount, token.blockchainType)
        }

        return getOrCreateWallet(token)
    }

    private suspend fun getOrCreateWallet(token: Token): Wallet {
        walletManager.activeWallets.find { it.token == token }?.let {
            return it
        }

        if (token.blockchainType == BlockchainType.Zcash || token.blockchainType == BlockchainType.Monero) {
            if (restoreSettingsManager.settings(activeAccount, token.blockchainType).birthdayHeight == null) {
                val settings = RestoreSettings().apply {
                    birthdayHeight = getBirthdayHeightForNewWallet(token.blockchainType)
                }
                restoreSettingsManager.save(settings, activeAccount, token.blockchainType)
            }
        }

        return createWallet(token)
    }

    private fun getBirthdayHeightForNewWallet(blockchainType: BlockchainType): Long? = when (blockchainType) {
        BlockchainType.Zcash -> zcashBirthdayProvider.getLatestCheckpointBlockHeight()
        BlockchainType.Monero -> moneroBirthdayProvider.restoreHeightForNewWallet()
        else -> null
    }

    private suspend fun createWallet(token: Token): Wallet {
        val wallet = Wallet(token, activeAccount)

        walletManager.save(listOf(wallet))

        Utils.waitUntil(1000L, 100L) {
            App.adapterManager.getReceiveAdapterForWallet(wallet) != null
        }

        return wallet
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
                fullCoinsProvider,
                App.adapterManager,
                App.currencyManager,
                App.marketKit,
                App.zcashBirthdayProvider,
                App.moneroBirthdayProvider,
                App.restoreSettingsManager
            ) as T
        }
    }
}

sealed interface CoinForReceiveType {
    data class Single(val wallet: Wallet) : CoinForReceiveType
    data class MultipleZcashAddressTypes(val wallet: Wallet) : CoinForReceiveType
    object MultipleDerivations : CoinForReceiveType
    object MultipleAddressTypes : CoinForReceiveType
    object MultipleBlockchains : CoinForReceiveType
}

data class ReceiveTokenSelectUiState(
    val fullCoins: List<FullCoin>,
    val searchQuery: String,
)
