package bitcoin.wallet.core.managers

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ILockManager
import bitcoin.wallet.core.ISecuredStorage
import bitcoin.wallet.core.IWordsManager
import bitcoin.wallet.modules.newpin.PinModule
import bitcoin.wallet.viewHelpers.DateHelper
import java.util.*



class LockManager(
        private val localStorage: ILocalStorage,
        private val securedStorage: ISecuredStorage,
        private val wordsManager: IWordsManager): ILockManager {

    private val lockTimeout: Double = 10.0

    override var isLocked: Boolean = false

    override fun didEnterBackground() {
        if (!wordsManager.isLoggedIn || isLocked) {
            return
        }

        localStorage.lastExitDate = Date().time
    }

    override fun willEnterForeground() {
        if (!wordsManager.isLoggedIn || isLocked || securedStorage.pinIsEmpty()) {
            return
        }

        val secondsAgo = DateHelper.getSecondsAgo(localStorage.lastExitDate)
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
