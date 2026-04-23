package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule.AccountViewItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class ManageAccountsViewModel(
    private val accountManager: IAccountManager,
    private val mode: ManageAccountsModule.Mode
) : ViewModel() {

    var viewItems by mutableStateOf<Pair<List<AccountViewItem>, List<AccountViewItem>>?>(null)
    var finish by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    private var query: String? = null
    private var accounts: List<Account> = emptyList()
    private var activeAccount: Account? = null


    init {
        viewModelScope.launch {
            accountManager.accountsFlowable.asFlow()
                .collect {
                    activeAccount = accountManager.activeAccount
                    accounts = it
                    updateViewItems()
                }
        }

        viewModelScope.launch {
            accountManager.activeAccountStateFlow
                .collect { activeAccountState ->
                    if (activeAccountState is ActiveAccountState.ActiveAccount) {
                        activeAccount = activeAccountState.account
                        accounts = accountManager.accounts
                        updateViewItems()
                    }
                }
        }

        activeAccount = accountManager.activeAccount
        accounts = accountManager.accounts
        updateViewItems()
    }

    private fun updateViewItems() {
        val currentQuery = query

        viewItems = accounts
            .filter {
                currentQuery.isNullOrEmpty() || it.name.contains(currentQuery, ignoreCase = true)
            }
            .sortedBy { it.name.lowercase() }
            .map { getViewItem(it, activeAccount) }
            .partition { !it.isWatchAccount }
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
            migrationRequired = account.nonStandard,
        )

    fun onSelect(accountViewItem: AccountViewItem) {
        accountManager.setActiveAccountId(accountViewItem.accountId)

        if (mode == ManageAccountsModule.Mode.Switcher) {
            finish = true
        }
    }

    fun updateFilter(q: String) {
        searchQuery = q
        query = q
        viewModelScope.launch {
            updateViewItems()
        }
    }
}
