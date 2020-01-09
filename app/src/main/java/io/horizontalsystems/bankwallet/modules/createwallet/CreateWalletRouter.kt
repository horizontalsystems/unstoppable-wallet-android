package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.SingleLiveEvent

class CreateWalletRouter : CreateWalletModule.IRouter {
    val startMainModuleLiveEvent = SingleLiveEvent<Unit>()
    val close = SingleLiveEvent<Unit>()

    override fun startMainModule() {
        startMainModuleLiveEvent.call()
    }

    override fun close() {
        close.call()
    }
}
