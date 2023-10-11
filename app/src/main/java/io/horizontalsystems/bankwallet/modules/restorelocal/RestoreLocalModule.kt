package io.horizontalsystems.bankwallet.modules.restorelocal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupViewItemFactory
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsViewModel.OtherBackupViewItem
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsViewModel.WalletBackupViewItem

object RestoreLocalModule {

    class Factory(private val backupJsonString: String?, private val fileName: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestoreLocalViewModel(backupJsonString, App.accountFactory, App.backupProvider, BackupViewItemFactory(), fileName) as T
        }
    }

    data class UiState(
        val passphraseState: DataState.Error?,
        val showButtonSpinner: Boolean,
        val parseError: Exception?,
        val showSelectCoins: AccountType?,
        val manualBackup: Boolean,
        val restored: Boolean,
        var walletBackupViewItems: List<WalletBackupViewItem>,
        var otherBackupViewItems: List<OtherBackupViewItem>,
        val showBackupItems: Boolean
    )
}