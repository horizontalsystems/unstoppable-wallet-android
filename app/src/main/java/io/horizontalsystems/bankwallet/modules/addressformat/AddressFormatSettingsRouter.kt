package io.horizontalsystems.bankwallet.modules.addressformat

import io.horizontalsystems.core.SingleLiveEvent

class AddressFormatSettingsRouter: AddressFormatSettingsModule.IRouter {

    val close = SingleLiveEvent<Unit>()

    override fun close() {
        close.call()
    }

}
