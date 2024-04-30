package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import io.horizontalsystems.bankwallet.core.stats.StatsManager
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenActivity
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.IPinComponent

class BackgroundStateChangeListener(
    private val pinComponent: IPinComponent,
    private val statsManager: StatsManager
) : BackgroundManager.Listener {

    override fun willEnterForeground(activity: Activity) {
        pinComponent.willEnterForeground(activity)

        statsManager.sendStats()
    }

    override fun didEnterBackground() {
        pinComponent.didEnterBackground()
    }

    override fun onAllActivitiesDestroyed() {
        pinComponent.lock()
    }

}
