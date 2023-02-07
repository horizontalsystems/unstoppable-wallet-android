package cash.p.terminal.modules.restoreaccount.restoreblockchains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.AccountType
import cash.p.terminal.modules.enablecoin.EnableCoinService
import cash.p.terminal.modules.enablecoin.coinplatforms.CoinTokensService
import cash.p.terminal.modules.enablecoin.coinplatforms.CoinTokensViewModel
import cash.p.terminal.modules.enablecoin.coinsettings.CoinSettingsService
import cash.p.terminal.modules.enablecoin.coinsettings.CoinSettingsViewModel
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import cash.p.terminal.modules.market.ImageSource
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Token

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
                App.marketKit,
                enableCoinService,
                App.evmBlockchainManager,
                App.evmTestnetManager
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
                    CoinTokensViewModel(coinTokensService, App.accountManager) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    class InternalItem(val blockchain: Blockchain, val token: Token)
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
