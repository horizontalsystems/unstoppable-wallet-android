package cash.p.terminal.modules.restorelocal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.backuplocal.fullbackup.BackupItems

object RestoreLocalModule {

    class Factory(private val backupJsonString: String?, private val fileName: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestoreLocalViewModel(backupJsonString, App.accountFactory, App.backupProvider, fileName) as T
        }
    }

    data class UiState(
        val passphraseState: DataState.Error?,
        val showButtonSpinner: Boolean,
        val parseError: Exception?,
        val showSelectCoins: AccountType?,
        val manualBackup: Boolean,
        val restored: Boolean,
        val backupItems: BackupItems?,
        val showBackupItems: Boolean
    )
}