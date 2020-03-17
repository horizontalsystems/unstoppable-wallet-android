package io.horizontalsystems.bankwallet.modules.blockchainsettings

import io.horizontalsystems.core.SingleLiveEvent

class BlockchainSettingsRouter: BlockchainSettingsModule.IRouter {

    val closeWithResultOk = SingleLiveEvent<Unit>()
    val close = SingleLiveEvent<Unit>()

    override fun closeWithResultOk() {
        closeWithResultOk.call()
    }

    override fun close() {
        close.call()
    }
}
