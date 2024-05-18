package cash.p.terminal.core.managers

import android.app.Activity
import cash.p.terminal.core.stats.StatsManager
import cash.p.terminal.modules.keystore.KeyStoreActivity
import cash.p.terminal.modules.lockscreen.LockScreenActivity
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
