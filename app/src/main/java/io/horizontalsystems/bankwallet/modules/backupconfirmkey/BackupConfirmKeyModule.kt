package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.RandomProvider
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Account

object BackupConfirmKeyModule {
    const val ACCOUNT = "account"

    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = BackupConfirmKeyService(account, App.accountManager, RandomProvider())

            return BackupConfirmKeyViewModel(service, Translator) as T
        }
    }

    fun prepareParams(account: Account) = bundleOf(ACCOUNT to account)

}
