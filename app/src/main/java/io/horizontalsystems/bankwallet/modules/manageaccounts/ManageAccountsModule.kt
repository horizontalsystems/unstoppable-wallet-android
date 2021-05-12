package io.horizontalsystems.bankwallet.modules.manageaccounts

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.findNavController
import kotlinx.android.parcel.Parcelize

object ManageAccountsModule {
    const val MODE = "mode"

    class Factory(private val mode: Mode) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = ManageAccountsService(App.accountManager)
            return ManageAccountsViewModel(service, mode, listOf(service)) as T
        }
    }

    fun start(fragment: Fragment, navigateTo: Int, navOptions: NavOptions, mode: Mode) {
        fragment.findNavController().navigate(navigateTo, bundleOf(MODE to mode), navOptions)
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
            val callback: () -> Unit
    )

    @Parcelize
    enum class Mode : Parcelable {
        Manage, Switcher
    }

}
