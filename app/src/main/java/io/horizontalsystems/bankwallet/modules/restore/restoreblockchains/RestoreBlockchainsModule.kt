package io.horizontalsystems.bankwallet.modules.restore.restoreblockchains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.blockchainLogo
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinPlatformsService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinPlatformsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin

object RestoreBlockchainsModule {

    class Factory(private val accountType: AccountType) : ViewModelProvider.Factory {

        private val restoreSettingsService by lazy {
            RestoreSettingsService(App.restoreSettingsManager)
        }
        private val coinSettingsService by lazy {
            CoinSettingsService()
        }
        private val coinPlatformsService by lazy {
            CoinPlatformsService()
        }
        private val enableCoinService by lazy {
            EnableCoinService(coinPlatformsService, restoreSettingsService, coinSettingsService)
        }

        private val restoreSelectCoinsService by lazy {
            RestoreBlockchainsService(
                accountType,
                App.accountFactory,
                App.accountManager,
                App.walletManager,
                App.coinManager,
                enableCoinService
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
                CoinPlatformsViewModel::class.java -> {
                    CoinPlatformsViewModel(coinPlatformsService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    enum class Blockchain {
        Bitcoin, Ethereum, BinanceSmartChain, BitcoinCash, Zcash, Litecoin, Dash, BinanceChain;

        val title: String
            get() = when (this) {
                Bitcoin -> "Bitcoin"
                Ethereum -> "Ethereum"
                BinanceSmartChain -> "Binance Smart Chain"
                BinanceChain -> "Binance Chain"
                BitcoinCash -> "Bitcoin Cash"
                Zcash -> "Zcash"
                Litecoin -> "Litecoin"
                Dash -> "Dash"
            }

        val description: String
            get() = when (this) {
                Bitcoin -> "BTC (BIP44, BIP49, BIP84)"
                Ethereum -> "ETH, ERC20 tokens"
                BinanceSmartChain -> "BNB, BEP20 tokens"
                BinanceChain -> "BNB, BEP2 tokens"
                BitcoinCash -> "BCH (Legacy, CashAddress)"
                Zcash -> "ZEC"
                Litecoin -> "LTC (BIP44, BIP49, BIP84)"
                Dash -> "DASH"
            }

        val coinType: CoinType
            get() = when (this) {
                Bitcoin -> CoinType.Bitcoin
                Ethereum -> CoinType.Ethereum
                BinanceSmartChain -> CoinType.BinanceSmartChain
                BinanceChain -> CoinType.Bep2("BNB")
                BitcoinCash -> CoinType.BitcoinCash
                Zcash -> CoinType.Zcash
                Litecoin -> CoinType.Litecoin
                Dash -> CoinType.Dash
            }

        val icon: ImageSource
             get() = ImageSource.Local(coinType.blockchainLogo)
    }

    class InternalItem(val blockchain: Blockchain, val platformCoin: PlatformCoin)
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
