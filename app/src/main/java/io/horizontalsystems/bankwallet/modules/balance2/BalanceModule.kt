package io.horizontalsystems.bankwallet.modules.balance2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.*
import io.horizontalsystems.marketkit.models.CoinPrice

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

    data class BalanceItem(
        val wallet: Wallet,
        val mainNet: Boolean,
        val balanceData: BalanceData,
        val state: AdapterState,
        val coinPrice: CoinPrice? = null
    ) {
        val fiatValue get() = coinPrice?.value?.let { balanceData.available.times(it) }
    }
}