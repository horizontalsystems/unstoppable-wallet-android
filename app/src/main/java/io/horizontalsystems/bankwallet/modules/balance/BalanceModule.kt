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

            val balanceService = BalanceService(
                App.walletManager,
                App.adapterManager,
                App.xRateManager,
                App.currencyManager,
                App.localStorage,
                BalanceSorter(),
                App.connectivityManager,
                App.feeCoinProvider,
                App.accountSettingManager
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

    data class BalanceItem(val wallet: Wallet, val mainNet: Boolean) {
        var balance: BigDecimal? = null
        var balanceLocked: BigDecimal? = null
        val balanceTotal: BigDecimal?
            get() {
                var result = balance ?: return null

                balanceLocked?.let { balanceLocked ->
                    result += balanceLocked
                }

                return result
            }

        var state: AdapterState? = null
        var latestRate: LatestRate? = null

        val fiatValue: BigDecimal?
            get() = latestRate?.rate?.let { balance?.times(it) }
    }
}
