package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsService
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsViewModel
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

    class Factory(private val predefinedAccountType: PredefinedAccountType) : ViewModelProvider.Factory {

        private val restoreSelectCoinsService by lazy {
            RestoreSelectCoinsService(predefinedAccountType, App.coinManager)
        }

        private val blockchainSettingsService by lazy {
            BlockchainSettingsService(App.derivationSettingsManager, App.bitcoinCashCoinTypeManager)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                RestoreSelectCoinsViewModel::class.java -> {
                    RestoreSelectCoinsViewModel(restoreSelectCoinsService, blockchainSettingsService, listOf(restoreSelectCoinsService)) as T
                }
                BlockchainSettingsViewModel::class.java -> {
                    BlockchainSettingsViewModel(blockchainSettingsService, StringProvider(App.instance)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
