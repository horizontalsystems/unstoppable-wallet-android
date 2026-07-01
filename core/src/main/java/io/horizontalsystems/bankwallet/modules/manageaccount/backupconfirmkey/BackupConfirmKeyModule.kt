package io.horizontalsystems.bankwallet.modules.manageaccount.backupconfirmkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.RandomProvider
import io.horizontalsystems.bankwallet.entities.Account

object BackupConfirmKeyModule {
    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupConfirmKeyViewModel(account, App.accountManager, RandomProvider()) as T
        }
    }
}
