package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account

object UnlinkConfirmationDialogModule {
    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UnlinkConfirmationDialogViewModel(account, App.accountManager) as T
        }
    }
}