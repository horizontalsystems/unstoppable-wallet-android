package cash.p.terminal.modules.manageaccounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.ActiveAccountState
import cash.p.terminal.wallet.Account
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class ManageAccountsViewModel(
    private val accountManager: cash.p.terminal.wallet.IAccountManager,
    private val mode: ManageAccountsModule.Mode
) : ViewModel() {

    var viewItems by mutableStateOf<Pair<List<AccountViewItem>, List<AccountViewItem>>?>(null)
    var finish by mutableStateOf(false)

    init {
        viewModelScope.launch {
            accountManager.accountsFlowable.asFlow()
                .collect {
                    updateViewItems(accountManager.activeAccount, it)
                }
        }

        viewModelScope.launch {
            accountManager.activeAccountStateFlow
                .collect { activeAccountState ->
                    if (activeAccountState is cash.p.terminal.wallet.ActiveAccountState.ActiveAccount) {
                        updateViewItems(activeAccountState.account, accountManager.accounts)
                    }
                }
        }

        updateViewItems(accountManager.activeAccount, accountManager.accounts)
    }

    private fun updateViewItems(activeAccount: cash.p.terminal.wallet.Account?, accounts: List<cash.p.terminal.wallet.Account>) {
        viewItems = accounts
            .sortedBy { it.name.lowercase() }
            .map { getViewItem(it, activeAccount) }
            .partition { !it.isWatchAccount }
    }

    private fun getViewItem(account: cash.p.terminal.wallet.Account, activeAccount: cash.p.terminal.wallet.Account?) =
        AccountViewItem(
            accountId = account.id,
            title = account.name,
            subtitle = account.type.detailedDescription,
            selected = account == activeAccount,
            backupRequired = !account.isBackedUp && !account.isFileBackedUp,
            showAlertIcon = !account.isBackedUp || account.nonStandard || account.nonRecommended,
            isWatchAccount = account.isWatchAccount,
            migrationRequired = account.nonStandard,
        )

    fun onSelect(accountViewItem: AccountViewItem) {
        accountManager.setActiveAccountId(accountViewItem.accountId)

        if (mode == ManageAccountsModule.Mode.Switcher) {
            finish = true
        }
    }
}
