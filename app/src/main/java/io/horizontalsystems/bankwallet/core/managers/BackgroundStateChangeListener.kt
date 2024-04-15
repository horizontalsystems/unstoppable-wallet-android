package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.IPinComponent

class BackgroundStateChangeListener(
    private val pinComponent: IPinComponent
) : BackgroundManager.Listener {

    override fun willEnterForeground(activity: Activity) {
        pinComponent.willEnterForeground(activity)
    }

    override fun didEnterBackground() {
        pinComponent.didEnterBackground()
    }

    override fun onAllActivitiesDestroyed() {
        pinComponent.lock()
    }

}
