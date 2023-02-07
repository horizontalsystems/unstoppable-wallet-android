package cash.p.terminal.modules.unlinkaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.Account

object UnlinkAccountModule {
    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UnlinkAccountViewModel(account, App.accountManager) as T
        }
    }
}