package cash.p.terminal.modules.backuplocal.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.PassphraseValidator
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.backuplocal.fullbackup.BackupProvider
import cash.p.terminal.modules.settings.appearance.AppIconService
import cash.p.terminal.modules.settings.appearance.LaunchScreenService
import cash.p.terminal.modules.theme.ThemeService

object BackupLocalPasswordModule {

    class Factory(private val accountId: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val backupProvider = BackupProvider(
                App.localStorage,
                App.languageManager,
                App.enabledWalletsStorage,
                App.restoreSettingsManager,
                App.accountManager,

                App.evmBlockchainManager,

                App.marketFavoritesManager,
                App.balanceViewTypeManager,
                AppIconService(App.localStorage),
                ThemeService(App.localStorage),
                App.chartIndicatorManager,
                App.balanceHiddenManager,
                App.baseTokenManager,
                LaunchScreenService(App.localStorage),
                App.currencyManager,

                App.btcBlockchainManager,
                App.evmSyncSourceManager,
                App.solanaRpcSourceManager,

                App.contactsRepository
            )
            return BackupLocalPasswordViewModel(
                PassphraseValidator(),
                App.accountManager,
                backupProvider,
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