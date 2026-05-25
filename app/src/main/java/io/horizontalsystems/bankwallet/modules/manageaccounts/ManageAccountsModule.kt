package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

object ManageAccountsModule {
    @Serializable
    data class Input(@Serializable(with = HSScreenKClassSerializer::class) val popOffOnSuccess: KClass<out HSPage>, val popOffInclusive: Boolean)

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
