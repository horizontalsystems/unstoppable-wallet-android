package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensService
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinTokensViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.market.ImageSource

object RestoreBlockchainsModule {

    class Factory(
        private val accountName: String,
        private val accountType: AccountType,
        private val manualBackup: Boolean,
        private val fileBackup: Boolean
    ) : ViewModelProvider.Factory {

        private val restoreSettingsService by lazy {
            RestoreSettingsService(App.restoreSettingsManager, App.zcashBirthdayProvider)
        }
        private val coinTokensService by lazy {
            CoinTokensService()
        }
        private val enableCoinService by lazy {
            EnableCoinService(coinTokensService, restoreSettingsService)
        }

        private val restoreSelectCoinsService by lazy {
            RestoreBlockchainsService(
                accountName,
                accountType,
                manualBackup,
                fileBackup,
                App.accountFactory,
                App.accountManager,
                App.walletManager,
                App.marketKit,
                enableCoinService,
                App.evmBlockchainManager,
                App.tokenAutoEnableManager
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
                RestoreBlockchainsViewModel::class.java -> {
                    RestoreBlockchainsViewModel(
                        restoreSelectCoinsService,
                        listOf(restoreSelectCoinsService)
                    ) as T
                }
                CoinTokensViewModel::class.java -> {
                    CoinTokensViewModel(coinTokensService, App.accountManager) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}

data class CoinViewItem<T>(
    val item: T,
    val imageSource: ImageSource,
    val title: String,
    val subtitle: String,
    val enabled: Boolean,
    val hasSettings: Boolean = false,
    val hasInfo: Boolean = false,
    val label: String? = null,
)
