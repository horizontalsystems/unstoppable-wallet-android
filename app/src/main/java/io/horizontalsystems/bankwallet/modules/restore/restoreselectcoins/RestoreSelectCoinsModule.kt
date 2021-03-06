package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsService
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoins.*
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Observable

object RestoreSelectCoinsModule {
    interface IService {
        val canRestore: Observable<Boolean>
        val stateObservable: Observable<RestoreSelectCoinsService.State>
        var state: RestoreSelectCoinsService.State
        val enabledCoins: List<Coin>

        fun enable(coin: Coin, derivationSetting: DerivationSetting? = null)
        fun disable(coin: Coin)
    }

    class Factory(private val predefinedAccountType: PredefinedAccountType, private val accountType: AccountType)
        : ViewModelProvider.Factory {

        private val enableCoinsService by lazy {
            EnableCoinsService(
                    App.buildConfigProvider,
                    EnableCoinsErc20Provider(App.networkManager),
                    EnableCoinsBep2Provider(App.buildConfigProvider),
                    EnableCoinsBep20Provider(App.networkManager, App.appConfigProvider.bscscanApiKey),
                    App.coinManager
            )
        }

        private val blockchainSettingsService by lazy {
            BlockchainSettingsService(App.derivationSettingsManager, App.bitcoinCashCoinTypeManager)
        }

        private val restoreSelectCoinsService by lazy {
            RestoreSelectCoinsService(predefinedAccountType, accountType, App.coinManager, enableCoinsService, blockchainSettingsService)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                RestoreSelectCoinsViewModel::class.java -> {
                    RestoreSelectCoinsViewModel(restoreSelectCoinsService, listOf(restoreSelectCoinsService)) as T
                }
                BlockchainSettingsViewModel::class.java -> {
                    BlockchainSettingsViewModel(blockchainSettingsService, StringProvider()) as T
                }
                EnableCoinsViewModel::class.java -> {
                    EnableCoinsViewModel(enableCoinsService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
