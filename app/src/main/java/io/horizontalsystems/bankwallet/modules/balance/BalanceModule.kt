package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.cex.BalanceCexRepositoryWrapper
import io.horizontalsystems.bankwallet.modules.balance.cex.BalanceCexSorter
import io.horizontalsystems.bankwallet.modules.balance.cex.BalanceCexViewModel
import io.horizontalsystems.marketkit.models.CoinPrice

object BalanceModule {
    class AccountsFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BalanceAccountsViewModel(App.accountManager) as T
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val totalService = TotalService(
                App.currencyManager,
                App.marketKit,
                App.baseTokenManager,
                App.balanceHiddenManager
            )
            return BalanceViewModel(
                BalanceService.getInstance(),
                BalanceViewItemFactory(),
                App.balanceViewTypeManager,
                TotalBalance(totalService, App.balanceHiddenManager),
                App.localStorage,
            ) as T
        }
    }

    class FactoryCex : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val totalService = TotalService(
                App.currencyManager,
                App.marketKit,
                App.baseTokenManager,
                App.balanceHiddenManager
            )

            return BalanceCexViewModel(
                TotalBalance(totalService, App.balanceHiddenManager),
                App.localStorage,
                App.balanceViewTypeManager,
                BalanceCexRepositoryWrapper(App.cexAssetManager),
                BalanceXRateRepository(App.currencyManager, App.marketKit),
                BalanceCexSorter(),
                App.cexProviderManager,
            ) as T
        }
    }

    data class BalanceItem(
        val wallet: Wallet,
        val balanceData: BalanceData,
        val state: AdapterState,
        val coinPrice: CoinPrice? = null
    ) {
        val fiatValue get() = coinPrice?.value?.let { balanceData.available.times(it) }
        val balanceFiatTotal get() = coinPrice?.value?.let { balanceData.total.times(it) }
    }
}