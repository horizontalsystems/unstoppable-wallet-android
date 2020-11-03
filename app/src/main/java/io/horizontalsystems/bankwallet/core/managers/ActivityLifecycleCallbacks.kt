package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.core.ITorManager
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsFragment
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity

class ActivityLifecycleCallbacks(
        private val torManager: ITorManager
) : Application.ActivityLifecycleCallbacks, TorManager.Listener {

    init {
        torManager.setListener(this)
    }

    private var foregroundActivity: Activity? = null

    //Application.ActivityLifecycleCallbacks

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityResumed(activity: Activity) {
        foregroundActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        foregroundActivity = null
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityStopped(activity: Activity) {}

    //TorManager.Listener

    override fun onStatusChange(torStatus: TorStatus) {
        if (torManager.isTorEnabled) {
            (foregroundActivity as? FragmentActivity)?.supportFragmentManager?.primaryNavigationFragment?.childFragmentManager?.fragments?.lastOrNull()?.let { fragment ->
                if (fragment !is PrivacySettingsFragment) {
                    foregroundActivity?.let { activity ->
                        val intent = Intent(activity, TorConnectionActivity::class.java)
                        activity.startActivity(intent)
                    }
                }
            }

        }
    }
}
