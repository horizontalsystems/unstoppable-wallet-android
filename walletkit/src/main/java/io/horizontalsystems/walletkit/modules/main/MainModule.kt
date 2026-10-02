package io.horizontalsystems.walletkit.modules.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Choreographer
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.ActionCompletedDelegate
import io.horizontalsystems.walletkit.modules.balance.OpenSendTokenSelect
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.walletconnect.WCManager
import kotlin.system.exitProcess

object MainModule {

    /** The activity-scoped [MainViewModel]; created on first use. */
    fun viewModel(owner: ViewModelStoreOwner): MainViewModel =
        ViewModelProvider(owner, Factory())[MainViewModel::class.java]

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
                ActionCompletedDelegate,
                App.lockGate,
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

    // Until the new process has run Application.onCreate, the system keeps showing the last
    // frame this process drew. Drawing the splash background first makes that frame match what
    // follows, instead of a frozen page with a dismissed dialog.
    fun restartProcess(activity: Activity) {
        val splash = View(activity).apply {
            setBackgroundColor(ContextCompat.getColor(activity, R.color.tyler))
        }
        activity.setContentView(splash)

        // The second frame callback runs only after the splash frame has been drawn
        val choreographer = Choreographer.getInstance()
        choreographer.postFrameCallback {
            choreographer.postFrameCallback {
                startAsNewTask(activity)
                exitProcess(0)
            }
        }
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

    enum class MainNavigation(val iconRes: Int, val titleRes: Int) {
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
        // A link that needs a signature arrived while a watch account was active.
        val showChangeWalletWarning: Boolean,
        val selectedTabItem: MainNavigation,
        // Render this tab change without animation. Travels in the same state emission as
        // selectedTabItem so the decision cannot race the lock state: a crossfade after the
        // lock fallback would keep the outgoing wallet tab composed while locked.
        val snapTabSwitch: Boolean = false,
    )
}

data class DeeplinkPage(
    val screen: HSPage
)
