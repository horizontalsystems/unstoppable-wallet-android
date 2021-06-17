package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object BalanceModule2 {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val activeAccountService = ActiveAccountService(App.accountManager)

            val balanceService = BalanceService(
                App.walletManager,
                App.adapterManager,
                App.xRateManager,
                App.currencyManager,
                App.localStorage,
                BalanceSorter(),
                App.connectivityManager
            )
            val rateAppService = RateAppService(App.rateAppManager)

            return BalanceViewModel2(
                balanceService,
                rateAppService,
                activeAccountService,
                BalanceViewItemFactory(),
                App.appConfigProvider.reportEmail
            ) as T
        }
    }
}
