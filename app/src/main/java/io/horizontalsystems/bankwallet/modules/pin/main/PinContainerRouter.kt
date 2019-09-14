package io.horizontalsystems.bankwallet.modules.pin.main

import io.horizontalsystems.bankwallet.SingleLiveEvent

class PinContainerRouter: PinContainerModule.Router {

    val closeApplication = SingleLiveEvent<Unit>()
    val closeActivity = SingleLiveEvent<Unit>()

    override fun closeActivity() {
        closeActivity.call()
    }

    override fun closeApplication() {
        closeApplication.call()
    }
}
