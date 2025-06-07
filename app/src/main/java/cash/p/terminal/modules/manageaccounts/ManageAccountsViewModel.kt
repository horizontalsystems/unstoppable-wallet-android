package cash.p.terminal.modules.manageaccounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.ActiveAccountState
import cash.p.terminal.wallet.IAccountManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class ManageAccountsViewModel(
    private val accountManager: IAccountManager,
    private val mode: ManageAccountsModule.Mode
) : ViewModel() {

    var regularAccountsState by mutableStateOf<List<AccountViewItem>?>(null)
    var watchAccountsState by mutableStateOf<List<AccountViewItem>?>(null)
    var hardwareAccountsState by mutableStateOf<List<AccountViewItem>?>(null)

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
                    if (activeAccountState is ActiveAccountState.ActiveAccount) {
                        updateViewItems(activeAccountState.account, accountManager.accounts)
                    }
                }
        }

        updateViewItems(accountManager.activeAccount, accountManager.accounts)
    }

    private fun updateViewItems(activeAccount: Account?, accounts: List<Account>) {
        val regularAccounts = mutableListOf<AccountViewItem>()
        val watchAccounts = mutableListOf<AccountViewItem>()
        val hardwareAccounts = mutableListOf<AccountViewItem>()
        accounts
            .sortedBy { it.name.lowercase() }
            .map { getViewItem(it, activeAccount) }
            .forEach { accountViewItem ->
                if (accountViewItem.isWatchAccount) {
                    watchAccounts.add(accountViewItem)
                } else if (accountViewItem.isHardwareWallet) {
                    hardwareAccounts.add(accountViewItem)
                } else {
                    regularAccounts.add(accountViewItem)
                }
            }

        regularAccountsState = regularAccounts
        watchAccountsState = watchAccounts
        hardwareAccountsState = hardwareAccounts
    }

    private fun getViewItem(account: Account, activeAccount: Account?) =
        AccountViewItem(
            accountId = account.id,
            title = account.name,
            subtitle = account.type.detailedDescription,
            selected = account == activeAccount,
            backupRequired = !account.isBackedUp && !account.isFileBackedUp,
            showAlertIcon = !account.isBackedUp || account.nonStandard || account.nonRecommended,
            isWatchAccount = account.isWatchAccount,
            isHardwareWallet = account.type is AccountType.HardwareCard,
            migrationRequired = account.nonStandard,
        )

    fun onSelect(accountViewItem: AccountViewItem) {
        accountManager.setActiveAccountId(accountViewItem.accountId)

        if (mode == ManageAccountsModule.Mode.Switcher) {
            finish = true
        }
    }
}
