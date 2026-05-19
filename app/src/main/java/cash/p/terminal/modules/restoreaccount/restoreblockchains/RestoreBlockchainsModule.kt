package cash.p.terminal.modules.restoreaccount.restoreblockchains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.LitecoinBirthdayProvider
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.core.managers.ZcashBirthdayProvider
import cash.p.terminal.modules.enablecoin.blockchaintokens.BlockchainTokensService
import cash.p.terminal.modules.enablecoin.blockchaintokens.BlockchainTokensViewModel
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import cash.p.terminal.ui_compose.components.ImageSource
import cash.p.terminal.wallet.AccountType
import org.koin.java.KoinJavaComponent.inject

object RestoreBlockchainsModule {

    class Factory(
        private val accountName: String,
        private val accountType: AccountType,
        private val manualBackup: Boolean,
        private val fileBackup: Boolean
    ) : ViewModelProvider.Factory {

        private val restoreSettingsManager: RestoreSettingsManager by inject(RestoreSettingsManager::class.java)
        private val zcashBirthdayProvider: ZcashBirthdayProvider by inject(ZcashBirthdayProvider::class.java)
        private val litecoinBirthdayProvider: LitecoinBirthdayProvider by inject(LitecoinBirthdayProvider::class.java)
        private val restoreSettingsService by lazy {
            RestoreSettingsService(restoreSettingsManager, zcashBirthdayProvider, litecoinBirthdayProvider)
        }
        private val blockchainTokensService by lazy {
            BlockchainTokensService()
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
                App.tokenAutoEnableManager,
                blockchainTokensService,
                restoreSettingsService
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
                BlockchainTokensViewModel::class.java -> {
                    BlockchainTokensViewModel(blockchainTokensService) as T
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
