package io.horizontalsystems.bankwallet.modules.coinsettings

import io.horizontalsystems.bankwallet.SingleLiveEvent

class CoinSettingsRouter: CoinSettingsModule.IRouter {

    val closeWithResultOk = SingleLiveEvent<Unit>()
    val close = SingleLiveEvent<Unit>()

    override fun closeWithResultOk() {
        closeWithResultOk.call()
    }

    override fun close() {
        close.call()
    }
}