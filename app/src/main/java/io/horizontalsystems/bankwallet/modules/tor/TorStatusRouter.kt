package io.horizontalsystems.bankwallet.modules.tor

import io.horizontalsystems.core.SingleLiveEvent

class TorStatusRouter: TorStatusModule.Router {

    val closeEvent = SingleLiveEvent<Unit>()
    val restartAppEvent = SingleLiveEvent<Unit>()

    override fun closeView() {
        closeEvent.call()
    }

    override fun restartApp() {
        restartAppEvent.call()
    }
}