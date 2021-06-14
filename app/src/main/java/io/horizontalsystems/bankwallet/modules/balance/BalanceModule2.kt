package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object BalanceModule2 {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val balanceService = BalanceService(
                App.accountManager,
                App.walletManager,
                App.adapterManager,
                App.xRateManager,
                App.currencyManager,
                App.localStorage,
                BalanceSorter()
            )
            return BalanceViewModel2(balanceService, BalanceViewItemFactory()) as T
        }
    }
}
