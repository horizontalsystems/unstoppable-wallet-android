package io.horizontalsystems.pin.edit

import io.horizontalsystems.core.SingleLiveEvent

class EditPinRouter : EditPinModule.IRouter {

    val dismissWithSuccess = SingleLiveEvent<Unit>()

    override fun dismissModuleWithSuccess() {
        dismissWithSuccess.call()
    }
}
