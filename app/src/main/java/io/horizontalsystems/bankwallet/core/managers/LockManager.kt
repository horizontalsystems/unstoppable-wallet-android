package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILockManager
import io.horizontalsystems.bankwallet.core.ISecuredStorage
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.reactivex.subjects.PublishSubject
import java.util.*

class LockManager(private val securedStorage: ISecuredStorage, private val authManager: AuthManager) : ILockManager {

    private val lockTimeout: Double = 60.0

    override val lockStateUpdatedSignal: PublishSubject<Unit> = PublishSubject.create()

    override var isLocked: Boolean = false
        set(value) {
            field = value
            lockStateUpdatedSignal.onNext(Unit)
        }

    override fun didEnterBackground() {
        if (isLocked && !authManager.isLoggedIn) {
            return
        }

        App.lastExitDate = Date().time
    }

    override fun willEnterForeground() {
        if (isLocked || !authManager.isLoggedIn || securedStorage.pinIsEmpty()) {
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
