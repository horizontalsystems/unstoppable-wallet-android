package cash.p.terminal.modules.backupconfirmkey

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.RandomProvider
import cash.p.terminal.entities.Account

object BackupConfirmKeyModule {
    const val ACCOUNT = "account"

    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupConfirmKeyViewModel(account, App.accountManager, RandomProvider()) as T
        }
    }

    fun prepareParams(account: Account) = bundleOf(ACCOUNT to account)

}
