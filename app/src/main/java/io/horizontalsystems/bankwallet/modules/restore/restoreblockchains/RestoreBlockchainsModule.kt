package io.horizontalsystems.bankwallet.modules.restore.restoreblockchains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.blockchainLogo
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.xxxkit.models.BlockchainType
import io.horizontalsystems.xxxkit.models.Token
import io.horizontalsystems.xxxkit.models.TokenQuery
import io.horizontalsystems.xxxkit.models.TokenType

object RestoreBlockchainsModule {

    class Factory(
        private val accountName: String,
        private val accountType: AccountType
        ) : ViewModelProvider.Factory {

        private val restoreSettingsService by lazy {
            RestoreSettingsService(App.restoreSettingsManager, App.zcashBirthdayProvider)
        }
        private val coinSettingsService by lazy {
            CoinSettingsService()
        }
        private val coinTokensService by lazy {
            CoinTokensService()
        }
        private val enableCoinService by lazy {
            EnableCoinService(coinTokensService, restoreSettingsService, coinSettingsService)
        }

        private val restoreSelectCoinsService by lazy {
            RestoreBlockchainsService(
                accountName,
                accountType,
                App.accountFactory,
                App.accountManager,
                App.walletManager,
                App.xxxKit,
                enableCoinService,
                App.evmBlockchainManager
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                RestoreSettingsViewModel::class.java -> {
                    RestoreSettingsViewModel(
                        restoreSettingsService,
                        listOf(restoreSettingsService)
                    ) as T
                }
                CoinSettingsViewModel::class.java -> {
                    CoinSettingsViewModel(coinSettingsService, listOf(coinSettingsService)) as T
                }
                RestoreBlockchainsViewModel::class.java -> {
                    RestoreBlockchainsViewModel(
                        restoreSelectCoinsService,
                        listOf(restoreSelectCoinsService)
                    ) as T
                }
                CoinTokensViewModel::class.java -> {
                    CoinTokensViewModel(coinTokensService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    sealed class Blockchain {
        object Bitcoin: Blockchain()
        object BitcoinCash: Blockchain()
        object Zcash: Blockchain()
        object Litecoin: Blockchain()
        object Dash: Blockchain()
        object BinanceChain: Blockchain();
        class Evm(val evmBlockchain: EvmBlockchain): Blockchain()

        val uid: String
            get() = when (this) {
                Bitcoin -> "Bitcoin"
                BitcoinCash -> "BitcoinCash"
                Zcash -> "Zcash"
                Litecoin -> "Litecoin"
                Dash -> "Dash"
                BinanceChain -> "BinanceChain"
                is Evm -> "EVM_${this.evmBlockchain.name}"
            }

        val title: String
            get() = when (this) {
                Bitcoin -> "Bitcoin"
                BitcoinCash -> "Bitcoin Cash"
                Zcash -> "Zcash"
                Litecoin -> "Litecoin"
                Dash -> "Dash"
                BinanceChain -> "Binance Chain"
                is Evm -> this.evmBlockchain.name
            }

        val description: String
            get() = when (this) {
                Bitcoin -> "BTC (BIP44, BIP49, BIP84)"
                BitcoinCash -> "BCH (Legacy, CashAddress)"
                Zcash -> "ZEC"
                Litecoin -> "LTC (BIP44, BIP49, BIP84)"
                Dash -> "DASH"
                BinanceChain -> "BNB, BEP2 tokens"
                is Evm -> this.evmBlockchain.description
            }

        val coinType: CoinType
            get() = when (this) {
                Bitcoin -> CoinType.Bitcoin
                BitcoinCash -> CoinType.BitcoinCash
                Zcash -> CoinType.Zcash
                Litecoin -> CoinType.Litecoin
                Dash -> CoinType.Dash
                BinanceChain -> CoinType.Bep2("BNB")
                is Evm -> this.evmBlockchain.baseCoinType
            }

        val tokenQuery: TokenQuery
            get() = when (this) {
                Bitcoin -> TokenQuery(BlockchainType.Bitcoin, TokenType.Native)
                BitcoinCash -> TokenQuery(BlockchainType.BitcoinCash, TokenType.Native)
                Zcash -> TokenQuery(BlockchainType.Zcash, TokenType.Native)
                Litecoin -> TokenQuery(BlockchainType.Litecoin, TokenType.Native)
                Dash -> TokenQuery(BlockchainType.Dash, TokenType.Native)
                BinanceChain -> TokenQuery(BlockchainType.BinanceChain, TokenType.Native)
                is Evm -> TokenQuery(this.evmBlockchain.blockchainType, TokenType.Native)
            }

        val icon: ImageSource
             get() = ImageSource.Local(coinType.blockchainLogo)

        companion object {

            val all: List<Blockchain>
                get() = listOf(
                    Bitcoin,
                    Evm(EvmBlockchain.Ethereum), Evm(EvmBlockchain.BinanceSmartChain), Evm(EvmBlockchain.Polygon),
                    Zcash, Dash, BitcoinCash, Litecoin, BinanceChain
                )

            fun getBlockchainByUid(uid: String): Blockchain? =
                all.firstOrNull { it.uid == uid }

        }
    }

    class InternalItem(val blockchain: Blockchain, val token: Token)
}

data class CoinViewItem(
    val uid: String,
    val imageSource: ImageSource,
    val title: String,
    val subtitle: String,
    val state: CoinViewItemState,
    val label: String? = null,
)

sealed class CoinViewItemState {
    data class ToggleVisible(val enabled: Boolean, val hasSettings: Boolean) : CoinViewItemState()
    object ToggleHidden : CoinViewItemState()
}
