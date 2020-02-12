package io.horizontalsystems.pin.unlock

import io.horizontalsystems.core.SingleLiveEvent

class UnlockPinRouter : UnlockPinModule.IRouter {

    val dismissWithSuccess = SingleLiveEvent<Unit>()

    override fun dismissModuleWithSuccess() {
        dismissWithSuccess.call()
    }
}
