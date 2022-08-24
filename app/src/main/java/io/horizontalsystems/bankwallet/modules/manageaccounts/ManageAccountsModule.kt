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
    const val popOffOnSuccessKey = "popOffOnSuccessKey"

    fun prepareParams(mode: Mode) = bundleOf(MODE to mode)
    fun prepareParams(popOffOnSuccess: Int) = bundleOf(popOffOnSuccessKey to popOffOnSuccess)

    class Factory(private val mode: Mode) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManageAccountsViewModel(App.accountManager, mode) as T
        }
    }

    data class AccountViewItem(
        val accountId: String,
        val title: String,
        val subtitle: String,
        val selected: Boolean,
        val backupRequired: Boolean,
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
