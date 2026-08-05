package io.horizontalsystems.walletkit.modules.balance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.collectWith
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.managers.ActiveAccountState
import io.horizontalsystems.walletkit.entities.AccountType

class BalanceAccountsViewModel(accountManager: IAccountManager) : ViewModel() {

    var balanceScreenState by mutableStateOf<BalanceScreenState?>(null)
        private set

    init {
        accountManager.activeAccountStateFlow.collectWith(viewModelScope) {
                handleAccount(it)
            }
    }

    private fun handleAccount(activeAccountState: ActiveAccountState) {
        when(activeAccountState) {
            ActiveAccountState.NotLoaded -> { }
            is ActiveAccountState.ActiveAccount -> {
                balanceScreenState = if (activeAccountState.account != null) {
                    BalanceScreenState.HasAccount(
                        AccountViewItem(
                            isWatchAccount = activeAccountState.account.isWatchAccount,
                            name = activeAccountState.account.name,
                            id = activeAccountState.account.id,
                            type = activeAccountState.account.type,
                            watchAddress = activeAccountState.account.watchAccountAddress
                        )
                    )
                } else {
                    BalanceScreenState.NoAccount
                }
            }
        }
    }
}

data class AccountViewItem(
    val isWatchAccount: Boolean,
    val name: String = "",
    val id: String,
    val type: AccountType,
    val watchAddress: String?
)

sealed class BalanceScreenState() {
    class HasAccount(val accountViewItem: AccountViewItem) : BalanceScreenState()
    object NoAccount : BalanceScreenState()
}
