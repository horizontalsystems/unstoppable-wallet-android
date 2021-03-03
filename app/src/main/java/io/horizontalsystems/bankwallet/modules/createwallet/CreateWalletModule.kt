package io.horizontalsystems.bankwallet.modules.createwallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Observable

object CreateWalletModule {

    interface IService{
        val stateAsync: Observable<CreateWalletService.State>
        val canCreateAsync: Observable<Boolean>
        var state: CreateWalletService.State

        fun enable(coin: Coin)
        fun disable(coin: Coin)
        fun create()
    }

    class Factory(private val predefinedAccountType: PredefinedAccountType?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = CreateWalletService(predefinedAccountType, App.predefinedAccountTypeManager, App.coinManager, App.accountCreator, App.accountManager, App.walletManager, App.derivationSettingsManager)

            return CreateWalletViewModel(service, listOf(service)) as T
        }
    }
}
