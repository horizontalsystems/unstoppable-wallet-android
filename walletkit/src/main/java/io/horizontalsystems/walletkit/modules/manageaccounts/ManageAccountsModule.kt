package io.horizontalsystems.walletkit.modules.manageaccounts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

object ManageAccountsModule {
    @Serializable
    data class Input(@Serializable(with = HSScreenKClassSerializer::class) val popOffOnSuccess: KClass<out HSPage>, val popOffInclusive: Boolean)

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
        val migrationRequired: Boolean,
    )

    data class ActionViewItem(
            @DrawableRes val icon: Int,
            @StringRes val title: Int,
            val callback: () -> Unit
    )

    @Serializable
    enum class Mode {
        Manage, Switcher
    }

}
