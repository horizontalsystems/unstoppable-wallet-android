package cash.p.terminal.modules.backuplocal.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.PassphraseValidator
import cash.p.terminal.entities.DataState

object BackupLocalPasswordModule {

    class Factory(private val accountId: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupLocalPasswordViewModel(
                PassphraseValidator(),
                App.accountManager,
                App.enabledWalletsStorage,
                App.restoreSettingsManager,
                accountId
            ) as T
        }
    }

    data class UiState(
        val passphraseState: DataState.Error?,
        val passphraseConfirmState: DataState.Error?,
        val showButtonSpinner: Boolean,
        val backupJson: String?,
        val closeScreen: Boolean,
        val showAccountIsNullError: Boolean
    )
}