package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILockManager
import io.horizontalsystems.bankwallet.core.IPinManager
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.reactivex.subjects.PublishSubject
import java.util.*

class LockManager(private val pinManager: IPinManager) : ILockManager, BackgroundManager.Listener {

    private val lockTimeout: Double = 60.0

    override val lockStateUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    override var isLocked: Boolean = false
        set(value) {
            field = value
            lockStateUpdatedSignal.onNext(Unit)
        }

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
