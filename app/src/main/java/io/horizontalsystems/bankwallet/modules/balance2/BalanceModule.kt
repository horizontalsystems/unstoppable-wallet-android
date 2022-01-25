package io.horizontalsystems.bankwallet.modules.balance2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.balance.*

object BalanceModule {
    class AccountsFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            val activeAccountService = ActiveAccountService(App.accountManager)

//            val rateAppService = RateAppService(App.rateAppManager)

            return BalanceAccountsViewModel(App.accountManager) as T
        }
    }

    class BalanceXxxFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            val activeAccountService = ActiveAccountService(App.accountManager)

//            val rateAppService = RateAppService(App.rateAppManager)

            val balanceService2 = BalanceService2(
                BalanceActiveWalletRepository(App.walletManager, App.accountSettingManager),
                BalanceXRateRepository(App.currencyManager, App.marketKit),
                BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao())),
                NetworkTypeChecker(App.accountSettingManager),
                App.localStorage,
                App.connectivityManager,
                BalanceSorter(),
                App.accountManager
            )


            return BalanceViewModel(
                balanceService2,
                BalanceViewItemFactory()
            ) as T
        }
    }

}