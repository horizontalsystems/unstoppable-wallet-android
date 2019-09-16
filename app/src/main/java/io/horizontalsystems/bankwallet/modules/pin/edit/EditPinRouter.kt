package io.horizontalsystems.bankwallet.modules.pin.edit

import io.horizontalsystems.bankwallet.SingleLiveEvent

class EditPinRouter: EditPinModule.Router {

    val dismissWithSuccess = SingleLiveEvent<Unit>()

    override fun dismissModuleWithSuccess() {
        dismissWithSuccess.call()
    }
}
