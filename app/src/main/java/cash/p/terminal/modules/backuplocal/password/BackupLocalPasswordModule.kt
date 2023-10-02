package cash.p.terminal.modules.backuplocal.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.PassphraseValidator
import cash.p.terminal.entities.DataState

object BackupLocalPasswordModule {

    class Factory(private val backupType: BackupType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupLocalPasswordViewModel(
                backupType,
                PassphraseValidator(),
                App.accountManager,
                App.backupProvider,
            ) as T
        }
    }

    data class UiState(
        val passphraseState: DataState.Error?,
        val passphraseConfirmState: DataState.Error?,
        val showButtonSpinner: Boolean,
        val backupJson: String?,
        val closeScreen: Boolean,
        val error: String?
//        val showAccountIsNullError: Boolean
    )
}