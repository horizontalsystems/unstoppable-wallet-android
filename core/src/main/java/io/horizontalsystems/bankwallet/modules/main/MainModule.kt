package io.horizontalsystems.bankwallet.modules.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.ActionCompletedDelegate
import io.horizontalsystems.bankwallet.modules.balance.OpenSendTokenSelect
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import kotlinx.parcelize.Parcelize

object MainModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                App.pinComponent,
                App.rateAppManager,
                App.backupManager,
                App.termsManager,
                App.accountManager,
                App.releaseNotesManager,
                App.donationShowManager,
                App.localStorage,
                App.wcSessionManager,
                App.wcManager,
                App.networkManager,
                ActionCompletedDelegate
            ) as T
        }
    }

    // MainActivity lives in :app; resolve its launcher intent without a compile-time
    // reference so this module stays in :core.
    private fun launchIntent(context: Context): Intent =
        context.packageManager.getLaunchIntentForPackage(context.packageName)!!

    fun start(context: Context, data: Uri? = null) {
        val intent = launchIntent(context)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.data = data
        context.startActivity(intent)
    }

    fun startAsNewTask(context: Context) {
        val intent = launchIntent(context)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    fun startAsNewTask(context: Activity) {
        val intent = launchIntent(context)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val options = androidx.core.app.ActivityOptionsCompat.makeCustomAnimation(
            context,
            0,  // No enter animation
            0   // No exit animation
        )

        context.startActivity(intent, options.toBundle())
    }

    sealed class BadgeType {
        object BadgeDot : BadgeType()
        class BadgeNumber(val number: Int) : BadgeType()
    }

    data class NavigationViewItem(
        val mainNavItem: MainNavigation,
        val selected: Boolean,
        val enabled: Boolean,
        val badge: BadgeType? = null
    )

    @Parcelize
    enum class MainNavigation(val iconRes: Int, val titleRes: Int) : Parcelable {
        Market(R.drawable.ic_market_24, R.string.Market_Title),
        Balance(R.drawable.ic_wallet_24, R.string.Balance_Title),
        Swap(R.drawable.ic_swap_filled_24, R.string.Swap),
//        Transactions(R.drawable.ic_transactions_24, R.string.Transactions_Title),
        Settings(R.drawable.ic_settings_24, R.string.Settings_Title);

        companion object {
            private val map = values().associateBy(MainNavigation::name)

            fun fromString(type: String?): MainNavigation? = map[type]
        }
    }

    data class UiState(
        val deeplinkPage: DeeplinkPage?,
        val mainNavItems: List<NavigationViewItem>,
        val showRateAppDialog: Boolean,
        val showWhatsNew: Boolean,
        val showDonationPage: Boolean,
        val torEnabled: Boolean,
        val wcSupportState: WCManager.SupportState?,
        val openSend: OpenSendTokenSelect?,
        val selectedTabItem: MainNavigation
    )
}

data class DeeplinkPage(
    val screen: HSPage
)
