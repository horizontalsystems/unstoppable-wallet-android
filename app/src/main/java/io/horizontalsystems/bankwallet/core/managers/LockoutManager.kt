package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILockoutManager
import io.horizontalsystems.bankwallet.entities.LockoutState

class LockoutManager: ILockoutManager {

    override var currentState: LockoutState = LockoutState.Unlocked(null)

    override fun didFailUnlock() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
