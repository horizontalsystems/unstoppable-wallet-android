package io.horizontalsystems.bankwallet.modules.contact

import io.horizontalsystems.bankwallet.SingleLiveEvent

class ContactRouter : ContactModule.IRouter {
    val sendEmailLiveEvent = SingleLiveEvent<String>()
    val openTelegramGroupEvent = SingleLiveEvent<String>()
    val openAppStatusLiveEvent = SingleLiveEvent<Unit>()

    override fun openSendMail(recipient: String) {
        sendEmailLiveEvent.postValue(recipient)
    }

    override fun openTelegram(group: String) {
        openTelegramGroupEvent.postValue(group)
    }

    override fun openAppStatus() {
        openAppStatusLiveEvent.postValue(Unit)
    }

}
