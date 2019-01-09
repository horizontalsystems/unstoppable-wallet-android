package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILockManager
import io.horizontalsystems.bankwallet.core.ISecuredStorage
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import java.util.*

class LockManager(
        private val securedStorage: ISecuredStorage,
        private val authManager: AuthManager): ILockManager {

    private val lockTimeout: Double = 60.0

    override var isLocked: Boolean = false

    override fun didEnterBackground() {
        if (!authManager.isLoggedIn || isLocked) {
            return
        }

        App.lastExitDate = Date().time
    }

    override fun willEnterForeground() {
        if (!authManager.isLoggedIn || isLocked || securedStorage.pinIsEmpty()) {
            return
        }

        val secondsAgo = DateHelper.getSecondsAgo(App.lastExitDate)
        if (secondsAgo < lockTimeout) {
            return
        }

        lock()
    }

    override fun lock() {
        isLocked = true
        PinModule.startForUnlock()
    }

    override fun onUnlock() {
        isLocked = false
    }
}
