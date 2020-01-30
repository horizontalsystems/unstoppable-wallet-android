package io.horizontalsystems.bankwallet.modules.blockchainsettings

import io.horizontalsystems.bankwallet.SingleLiveEvent

class BlockchainSettingsRouter: CoinSettingsModule.IRouter {

    val closeWithResultOk = SingleLiveEvent<Unit>()
    val close = SingleLiveEvent<Unit>()

    override fun closeWithResultOk() {
        closeWithResultOk.call()
    }

    override fun close() {
        close.call()
    }
}