package io.horizontalsystems.bankwallet.modules.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.xrateskit.entities.LatestRate
import java.math.BigDecimal

object BalanceModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val activeAccountService = ActiveAccountService(App.accountManager)

            val activeWalletRepository = ActiveWalletRepository(App.walletManager, App.accountSettingManager)
            val balanceItemRepositoryWithAdapter = BalanceItemWithAdapterRepository(
                activeWalletRepository,
                App.adapterManager,
                NetworkTypeChecker(App.accountSettingManager),
                BalanceCache()
            )
            val balanceItemXRateRepository = BalanceItemXRateRepository(
                balanceItemRepositoryWithAdapter,
                App.xRateManager,
                App.currencyManager,
                App.feeCoinProvider
            )

            val balanceConfigurator = BalanceConfigurator(App.localStorage)
            val balanceItemRepositorySorted = BalanceItemSortedRepository(balanceItemXRateRepository, BalanceSorter(),
                balanceConfigurator
            )

            val balanceService = BalanceService(
                App.adapterManager,
                App.currencyManager,
                App.connectivityManager,
                balanceItemRepositorySorted,
                balanceConfigurator
            )

            val rateAppService = RateAppService(App.rateAppManager)

            return BalanceViewModel(
                balanceService,
                rateAppService,
                activeAccountService,
                BalanceViewItemFactory(),
                App.appConfigProvider.reportEmail
            ) as T
        }
    }

    data class BalanceItem(
        val wallet: Wallet,
        val mainNet: Boolean,
        val balance: BigDecimal? = null,
        val balanceLocked: BigDecimal? = null,
        val state: AdapterState? = null,
        val latestRate: LatestRate? = null
    ) {
        val balanceTotal: BigDecimal?
            get() {
                var result = balance ?: return null

                balanceLocked?.let { balanceLocked ->
                    result += balanceLocked
                }

                return result
            }


        val fiatValue: BigDecimal?
            get() = latestRate?.rate?.let { balance?.times(it) }
    }
}
