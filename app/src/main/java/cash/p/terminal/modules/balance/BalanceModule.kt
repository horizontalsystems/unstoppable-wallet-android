package cash.p.terminal.modules.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.modules.address.AddressHandlerFactory
import cash.p.terminal.modules.balance.cex.BalanceCexRepositoryWrapper
import cash.p.terminal.modules.balance.cex.BalanceCexSorter
import cash.p.terminal.modules.balance.cex.BalanceCexViewModel
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.wallet.balance.BalanceWarning

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
                currencyManager = App.currencyManager,
                marketKit = App.marketKit,
                baseTokenManager = App.baseTokenManager,
                balanceHiddenManager = App.balanceHiddenManager
            )
            return BalanceViewModel(
                service = DefaultBalanceService.getInstance("wallet"),
                balanceViewItemFactory = BalanceViewItemFactory(),
                balanceViewTypeManager = App.balanceViewTypeManager,
                totalBalance = TotalBalance(totalService, App.balanceHiddenManager),
                localStorage = App.localStorage,
                wCManager = App.wcManager,
                addressHandlerFactory = AddressHandlerFactory(App.appConfigProvider.udnApiKey),
                priceManager = App.priceManager
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
                BalanceViewItemFactory(),
                BalanceCexRepositoryWrapper(App.cexAssetManager, App.connectivityManager),
                DefaultBalanceXRateRepository("wallet", App.currencyManager, App.marketKit),
                BalanceCexSorter(),
                App.cexProviderManager,
            ) as T
        }
    }

    val BalanceWarning.warningText: WarningText
        get() = when (this) {
            BalanceWarning.TronInactiveAccountWarning -> WarningText(
                title = TranslatableString.ResString(R.string.Tron_TokenPage_AddressNotActive_Title),
                text = TranslatableString.ResString(R.string.Tron_TokenPage_AddressNotActive_Info),
            )
        }
}