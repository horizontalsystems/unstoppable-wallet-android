package cash.p.terminal.modules.managewallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.enablecoin.EnableCoinService
import cash.p.terminal.modules.enablecoin.coinplatforms.CoinTokensService
import cash.p.terminal.modules.enablecoin.coinplatforms.CoinTokensViewModel
import cash.p.terminal.modules.enablecoin.coinsettings.CoinSettingsService
import cash.p.terminal.modules.enablecoin.coinsettings.CoinSettingsViewModel
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsViewModel

object ManageWalletsModule {

    class Factory : ViewModelProvider.Factory {

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

        private val manageWalletsService by lazy {
            ManageWalletsService(
                App.marketKit,
                App.walletManager,
                App.accountManager,
                enableCoinService,
                App.restoreSettingsManager,
                App.evmTestnetManager
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
                ManageWalletsViewModel::class.java -> {
                    ManageWalletsViewModel(manageWalletsService, listOf(manageWalletsService)) as T
                }
                CoinTokensViewModel::class.java -> {
                    CoinTokensViewModel(coinTokensService, App.accountManager) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
