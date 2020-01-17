package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import io.horizontalsystems.bankwallet.SingleLiveEvent

class RestoreCoinsRouter : RestoreCoinsModule.IRouter {
    val startMainModuleLiveEvent = SingleLiveEvent<Unit>()
    val close = SingleLiveEvent<Unit>()

    override fun startMainModule() {
        startMainModuleLiveEvent.call()
    }

    override fun close() {
        close.call()
    }
}
