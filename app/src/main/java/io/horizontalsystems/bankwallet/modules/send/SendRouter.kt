package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.SingleLiveEvent

class SendRouter : SendModule.IRouter {

    val closeWithSuccess = SingleLiveEvent<Unit>()

    override fun scanQrCode() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeWithSuccess() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}