package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.core.SingleLiveEvent

class ReceiveRouter: ReceiveModule.IRouter {

    val shareAddress = SingleLiveEvent<String>()

    override fun shareAddress(address: String) {
        shareAddress.postValue(address)
    }
}
