package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.ILockManager
import io.horizontalsystems.core.IPinManager
import io.horizontalsystems.core.helpers.DateHelper
import java.util.*

class LockManager(private val pinManager: IPinManager) : ILockManager, BackgroundManager.Listener {

    private val lockTimeout: Double = 60.0

    override var isLocked: Boolean = false

    override fun didEnterBackground() {
        if (isLocked) {
            return
        }

        App.lastExitDate = Date().time
    }

    override fun willEnterForeground(activity: Activity) {
        if (isLocked || !pinManager.isPinSet) {
            return
        }

        val secondsAgo = DateHelper.getSecondsAgo(App.lastExitDate)
        if (secondsAgo > lockTimeout) {
            isLocked = true
        }
    }

    override fun onUnlock() {
        isLocked = false
    }
}
