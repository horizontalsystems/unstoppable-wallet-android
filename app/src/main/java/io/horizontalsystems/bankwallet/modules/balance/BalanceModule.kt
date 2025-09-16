package io.horizontalsystems.bankwallet.modules.balance

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerFactory
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
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
                App.balanceHiddenManager,
                App.localStorage,
            )
            return BalanceViewModel(
                BalanceService.getInstance("wallet"),
                BalanceViewItemFactory(),
                App.balanceViewTypeManager,
                App.localStorage,
                App.wcManager,
                AddressHandlerFactory(App.appConfigProvider.udnApiKey),
                App.priceManager,
                App.adapterManager,
                App.instance.isSwapEnabled,
                totalService
            ) as T
        }
    }

    data class BalanceItem(
        val wallet: Wallet,
        val balanceData: BalanceData,
        val state: AdapterState,
        val coinPrice: CoinPrice?,
        val warning: BalanceWarning? = null
    ) {
        val balanceFiatTotal by lazy {
            coinPrice?.value?.let { balanceData.total.times(it) }
        }
    }

    sealed class BalanceWarning : Warning() {
        data object TronInactiveAccountWarning : BalanceWarning()
    }

    val BalanceWarning.warningText: WarningText
        get() = when (this) {
            BalanceWarning.TronInactiveAccountWarning -> WarningText(
                title = TranslatableString.ResString(R.string.Tron_TokenPage_AddressNotActive_Title),
                text = TranslatableString.ResString(R.string.Tron_TokenPage_AddressNotActive_Info),
            )
        }
}

private fun BalanceContextMenuItem.toDropDownItem(isEnabled: Boolean = true): BalanceContextMenuViewItem {
    return BalanceContextMenuViewItem(
        item = this,
        isEnabled = isEnabled
    )
}

val BalanceViewItem2.contextMenuItems: List<BalanceContextMenuViewItem>
    get() {
        return if (this.isWatchAccount) {
            listOf(
                BalanceContextMenuItem.CopyAddress.toDropDownItem(),
                BalanceContextMenuItem.HideToken.toDropDownItem(),
                BalanceContextMenuItem.CoinInfo.toDropDownItem()
            )
        } else {
            listOf(
                BalanceContextMenuItem.Send.toDropDownItem(),
                BalanceContextMenuItem.CopyAddress.toDropDownItem(),
                BalanceContextMenuItem.Swap.toDropDownItem(),
                BalanceContextMenuItem.CoinInfo.toDropDownItem(),
                BalanceContextMenuItem.HideToken.toDropDownItem()
            )
        }
    }

enum class BalanceContextMenuItem(@StringRes val title: Int, @DrawableRes val icon: Int) {
    Send(R.string.Balance_Send, R.drawable.ic_arrow_up_24),
    CopyAddress(R.string.Button_CopyAddress, R.drawable.ic_copy_24),
    Swap(R.string.Swap, R.drawable.ic_swap_circle_24),
    CoinInfo(R.string.Coin_Info, R.drawable.ic_coin_info_24),
    HideToken(R.string.Button_HideCoin, R.drawable.ic_minus_24),
}

data class BalanceContextMenuViewItem(
    val item: BalanceContextMenuItem,
    val isEnabled: Boolean = true
)