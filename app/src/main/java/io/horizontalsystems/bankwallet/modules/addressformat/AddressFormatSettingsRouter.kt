package io.horizontalsystems.bankwallet.modules.addressformat

import io.horizontalsystems.core.SingleLiveEvent

class AddressFormatSettingsRouter: AddressFormatSettingsModule.IRouter {

    val closeWithResultOk = SingleLiveEvent<Unit>()
    val close = SingleLiveEvent<Unit>()

    override fun closeWithResultOk() {
        closeWithResultOk.call()
    }

    override fun close() {
        close.call()
    }

}
