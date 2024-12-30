package cash.p.terminal.modules.manageaccount.backupconfirmkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.RandomProvider
import cash.p.terminal.wallet.Account

object BackupConfirmKeyModule {
    class Factory(private val account: cash.p.terminal.wallet.Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupConfirmKeyViewModel(account, App.accountManager, RandomProvider()) as T
        }
    }
}
