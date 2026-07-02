package io.horizontalsystems.core.modules.restorelocal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.entities.AccountType
import io.horizontalsystems.core.entities.DataState
import io.horizontalsystems.core.modules.backuplocal.fullbackup.BackupViewItemFactory
import io.horizontalsystems.core.modules.backuplocal.fullbackup.SelectBackupItemsViewModel.OtherBackupViewItem
import io.horizontalsystems.core.modules.backuplocal.fullbackup.SelectBackupItemsViewModel.WalletBackupViewItem

object RestoreLocalModule {

    class Factory(
        private val backupJsonString: String?,
        private val fileName: String?,
        private val statPage: StatPage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestoreLocalViewModel(
                backupJsonString = backupJsonString,
                accountFactory = App.accountFactory,
                backupProvider = App.backupProvider,
                backupViewItemFactory = BackupViewItemFactory(),
                statPage = statPage,
                fileName = fileName
            ) as T
        }
    }

    data class UiState(
        val passphraseState: DataState.Error?,
        val showButtonSpinner: Boolean,
        val parseError: Exception?,
        val showSelectCoins: AccountType?,
        val manualBackup: Boolean,
        val restored: Boolean,
        val walletBackupViewItems: List<WalletBackupViewItem>,
        val otherBackupViewItems: List<OtherBackupViewItem>,
        val showBackupItems: Boolean,
        val hasSelection: Boolean,
    )
}