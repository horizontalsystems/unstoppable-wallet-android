package io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SelectBackupItemsViewModel(
    private val backupProvider: BackupProvider,
    private val backupViewItemFactory: BackupViewItemFactory
) : ViewModel() {

    private var viewState: ViewState = ViewState.Loading
    private var wallets: List<WalletBackupViewItem> = emptyList()
    private var otherBackupItems: List<OtherBackupViewItem> = emptyList()

    var uiState by mutableStateOf(
        UIState(
            viewState = viewState,
            wallets = wallets,
            otherBackupItems = otherBackupItems
        )
    )
        private set

    val selectedWallets: List<String>
        get() = wallets.filter { it.selected }.map { it.account.id }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val backupItems = backupProvider.fullBackupItems()
            val viewItems = backupViewItemFactory.backupViewItems(backupItems)

            wallets = viewItems.first
            otherBackupItems = viewItems.second
            viewState = ViewState.Success

            emitState()
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = UIState(viewState, wallets, otherBackupItems)
        }
    }

    fun toggle(wallet: WalletBackupViewItem) {
        wallets = wallets.map {
            if (wallet.account.id == it.account.id) {
                it.copy(selected = !wallet.selected)
            } else {
                it
            }
        }

        emitState()
    }

    data class UIState(
        val viewState: ViewState,
        val wallets: List<WalletBackupViewItem>,
        val otherBackupItems: List<OtherBackupViewItem>
    )

    data class WalletBackupViewItem(
        val account: Account,
        val name: String,
        val type: String,
        val backupRequired: Boolean,
        val selected: Boolean
    )

    data class OtherBackupViewItem(
        val title: String,
        val value: String? = null,
        val subtitle: String? = null
    )

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SelectBackupItemsViewModel(App.backupProvider, BackupViewItemFactory()) as T
        }
    }

}
