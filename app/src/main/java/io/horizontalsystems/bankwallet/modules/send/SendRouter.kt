package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.SingleLiveEvent

class SendRouter : SendModule.IRouter {

    val closeWithSuccess = SingleLiveEvent<Unit>()
    val scanQrCode = SingleLiveEvent<Unit>()

    override fun scanQrCode() {
        scanQrCode.call()
    }

    override fun closeWithSuccess() {
        closeWithSuccess.call()
    }
}