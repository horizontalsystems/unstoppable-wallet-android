package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.core.SingleLiveEvent

class CreateWalletRouter : CreateWalletModule.IRouter {
    val startMainModuleLiveEvent = SingleLiveEvent<Unit>()
    val showSuccessAndClose = SingleLiveEvent<Unit>()

    override fun startMainModule() {
        startMainModuleLiveEvent.call()
    }

    override fun showSuccessAndClose() {
        showSuccessAndClose.call()
    }
}
