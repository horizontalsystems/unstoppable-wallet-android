package io.horizontalsystems.bankwallet.modules.info

import io.horizontalsystems.core.SingleLiveEvent

class InfoRouter : InfoModule.IRouter {
    val goBackLiveEvent = SingleLiveEvent<Unit>()

    override fun goBack() {
        goBackLiveEvent.call()
    }
}
