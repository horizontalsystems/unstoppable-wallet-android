package io.horizontalsystems.bankwallet.modules.pin.set

import io.horizontalsystems.bankwallet.SingleLiveEvent

class SetPinRouter: SetPinModule.IRouter {

    val navigateToMain = SingleLiveEvent<Unit>()
    val dismissWithSuccess = SingleLiveEvent<Unit>()

    override fun dismissModuleWithSuccess() {
        dismissWithSuccess.call()
    }

    override fun navigateToMain() {
        navigateToMain.call()
    }
}
