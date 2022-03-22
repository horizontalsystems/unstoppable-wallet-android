package io.horizontalsystems.bankwallet.modules.manageaccounts

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import kotlinx.parcelize.Parcelize

object ManageAccountsModule {
    const val MODE = "mode"

    class Factory(private val mode: Mode) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = ManageAccountsService(App.accountManager)
            return ManageAccountsViewModel(service, mode, listOf(service)) as T
        }
    }

    fun prepareParams(mode: Mode) = bundleOf(MODE to mode)

    data class AccountViewItem(
        val accountId: String,
        val title: String,
        val subtitle: String,
        val selected: Boolean,
        val alert: Boolean,
        val isWatchAccount: Boolean
    )

    data class ActionViewItem(
            @DrawableRes val icon: Int,
            @StringRes val title: Int,
            val callback: () -> Unit
    )

    @Parcelize
    enum class Mode : Parcelable {
        Manage, Switcher
    }

}
