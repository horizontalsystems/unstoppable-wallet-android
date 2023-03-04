package io.horizontalsystems.bankwallet.modules.manageaccount

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.FaqManager

object ManageAccountModule {
    const val ACCOUNT_ID_KEY = "account_id_key"

    class Factory(private val accountId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = ManageAccountService(accountId, App.accountManager)

            return ManageAccountViewModel(service, listOf(service), FaqManager, App.languageManager) as T
        }
    }

    fun prepareParams(accountId: String) = bundleOf(ACCOUNT_ID_KEY to accountId)

}
