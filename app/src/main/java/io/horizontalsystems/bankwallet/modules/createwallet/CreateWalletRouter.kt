package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.SingleLiveEvent

class CreateWalletRouter : CreateWalletModule.IRouter {
    val startMainModuleLiveEvent = SingleLiveEvent<Unit>()

    override fun startMainModule() {
        startMainModuleLiveEvent.call()
    }
}
