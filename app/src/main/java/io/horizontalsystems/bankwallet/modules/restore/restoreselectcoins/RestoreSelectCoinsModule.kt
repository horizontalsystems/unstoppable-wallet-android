package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.reactivex.Observable

object RestoreSelectCoinsModule {
    interface IService{
        val canRestore: Observable<Boolean>
        var state: RestoreSelectCoinsService.State
        val enabledCoins: List<Coin>

        fun enable(coin: Coin, derivationSetting: DerivationSetting? = null)
        fun disable(coin: Coin)
    }

    class Factory(private val predefinedAccountType: PredefinedAccountType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = RestoreSelectCoinsService(predefinedAccountType, App.coinManager, App.derivationSettingsManager)

            return RestoreSelectCoinsViewModel(service, listOf(service)) as T
        }
    }
}
