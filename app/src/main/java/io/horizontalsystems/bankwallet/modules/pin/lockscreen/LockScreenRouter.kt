package io.horizontalsystems.bankwallet.modules.pin.lockscreen

import io.horizontalsystems.bankwallet.SingleLiveEvent

class LockScreenRouter: LockScreenModule.IRouter {

    val closeApplication = SingleLiveEvent<Unit>()
    val closeActivity = SingleLiveEvent<Unit>()

    override fun closeActivity() {
        closeActivity.call()
    }

    override fun closeApplication() {
        closeApplication.call()
    }
}
