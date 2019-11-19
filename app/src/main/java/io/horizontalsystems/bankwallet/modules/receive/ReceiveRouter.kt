package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.SingleLiveEvent

class ReceiveRouter: ReceiveModule.IRouter {

    val shareAddress = SingleLiveEvent<String>()

    override fun shareAddress(address: String) {
        shareAddress.postValue(address)
    }
}