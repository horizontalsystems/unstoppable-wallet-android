package io.horizontalsystems.bankwallet.modules.base

import io.horizontalsystems.core.SingleLiveEvent

class BaseView: BaseModule.View {

    val torConnectionStatus = SingleLiveEvent<Unit>()

    override fun showTorConnectionStatus() {
        torConnectionStatus.call()
    }
}