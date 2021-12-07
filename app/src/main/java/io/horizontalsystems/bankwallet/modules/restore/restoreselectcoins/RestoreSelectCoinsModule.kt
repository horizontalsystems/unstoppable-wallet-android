package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinPlatformsService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinPlatformsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsBep2Provider
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsEip20Provider
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsService
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsViewModel

object RestoreSelectCoinsModule {

    class Factory(private val accountType: AccountType) : ViewModelProvider.Factory {

        private val enableCoinsService by lazy {
            EnableCoinsService(
                App.instance.testMode,
                EnableCoinsBep2Provider(App.instance.testMode),
                EnableCoinsEip20Provider(
                    App.networkManager,
                    EnableCoinsEip20Provider.EnableCoinMode.Erc20
                ),
                EnableCoinsEip20Provider(
                    App.networkManager,
                    EnableCoinsEip20Provider.EnableCoinMode.Bep20
                )
            )
        }

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
            RestoreSelectCoinsService(
                accountType,
                App.accountFactory,
                App.accountManager,
                App.walletManager,
                App.coinManager,
                enableCoinsService,
                enableCoinService
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                RestoreSettingsViewModel::class.java -> {
                    RestoreSettingsViewModel(restoreSettingsService, listOf(restoreSettingsService)) as T
                }
                CoinSettingsViewModel::class.java -> {
                    CoinSettingsViewModel(coinSettingsService, listOf(coinSettingsService)) as T
                }
                EnableCoinsViewModel::class.java -> {
                    EnableCoinsViewModel(enableCoinsService) as T
                }
                RestoreSelectCoinsViewModel::class.java -> {
                    RestoreSelectCoinsViewModel(restoreSelectCoinsService, listOf(restoreSelectCoinsService)) as T
                }
                CoinPlatformsViewModel::class.java -> {
                    CoinPlatformsViewModel(coinPlatformsService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}

