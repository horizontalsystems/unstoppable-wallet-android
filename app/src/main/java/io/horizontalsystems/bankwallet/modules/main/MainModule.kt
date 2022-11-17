package io.horizontalsystems.bankwallet.modules.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.utils.RootUtil
import kotlinx.parcelize.Parcelize

object MainModule {

    class Factory(private val wcDeepLink: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = MainService(RootUtil, App.localStorage)
            return MainViewModel(
                App.pinComponent,
                App.rateAppManager,
                App.backupManager,
                App.termsManager,
                App.accountManager,
                App.releaseNotesManager,
                service,
                App.torKitManager,
                App.wc2SessionManager,
                App.wc1Manager,
                wcDeepLink
            ) as T
        }
    }

    fun start(context: Context, data: Uri? = null) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = data
        context.startActivity(intent)
    }

    fun startAsNewTask(context: Activity) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        context.overridePendingTransition(0, 0)
    }

    sealed class BadgeType {
        object BadgeDot : BadgeType()
        class BadgeNumber(val number: Int) : BadgeType()
    }

    @Parcelize
    enum class MainTab : Parcelable {
        Market,
        Balance,
        Transactions,
        Settings;

        companion object {
            private val map = values().associateBy(MainTab::name)

            fun fromString(type: String?): MainTab? = map[type]
        }
    }
}
