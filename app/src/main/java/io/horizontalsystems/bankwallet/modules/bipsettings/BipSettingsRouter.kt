package io.horizontalsystems.bankwallet.modules.bipsettings

import io.horizontalsystems.core.SingleLiveEvent

class BipSettingsRouter: BipSettingsModule.IRouter {

    val closeWithResultOk = SingleLiveEvent<Unit>()
    val close = SingleLiveEvent<Unit>()

    override fun closeWithResultOk() {
        closeWithResultOk.call()
    }

    override fun close() {
        close.call()
    }

}
