package cash.p.terminal.modules.manageaccounts

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import kotlinx.parcelize.Parcelize

object ManageAccountsModule {
    @Parcelize
    data class Input(val popOffOnSuccess: Int, val popOffInclusive: Boolean) : Parcelable

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
        val showAlertIcon: Boolean,
        val isWatchAccount: Boolean,
        val isHardwareWallet: Boolean,
        val migrationRequired: Boolean,
    )

    data class ActionViewItem(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        val enabled: Boolean = true,
        val callback: () -> Unit
    )

    @Parcelize
    enum class Mode : Parcelable {
        Manage, Switcher
    }

}
