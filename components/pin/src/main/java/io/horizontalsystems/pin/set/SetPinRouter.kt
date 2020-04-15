package io.horizontalsystems.pin.set

import io.horizontalsystems.core.SingleLiveEvent

class SetPinRouter : SetPinModule.IRouter {

    val dismissWithSuccess = SingleLiveEvent<Unit>()

    override fun dismissModuleWithSuccess() {
        dismissWithSuccess.call()
    }

}
