package io.horizontalsystems.bankwallet.modules.torpage

import io.horizontalsystems.core.SingleLiveEvent

class TorPageRouter: TorPageModule.IRouter {

    val closePage = SingleLiveEvent<Void>()

    override fun close() {
        closePage.call()
    }
}
