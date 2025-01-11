package cash.p.terminal.modules.manageaccount.backupkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.wallet.Account

object BackupKeyModule {
    class Factory(private val account: cash.p.terminal.wallet.Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupKeyViewModel(account) as T
        }
    }
}
