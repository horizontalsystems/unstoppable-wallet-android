package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object ManageAccountsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {

            val service = ManageAccountsService(App.accountManager)

            return ManageAccountsViewModel(service) as T
        }
    }

    data class AccountViewItem(
            val accountId: String,
            val title: String,
            val subtitle: String,
            val selected: Boolean,
            val alert: Boolean
    )

    data class ActionViewItem(
            @DrawableRes val icon: Int,
            @StringRes val title: Int,
            val callback: ()-> Unit
    )
}
