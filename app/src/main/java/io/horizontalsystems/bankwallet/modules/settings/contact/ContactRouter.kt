package io.horizontalsystems.bankwallet.modules.settings.contact

import io.horizontalsystems.core.SingleLiveEvent

class ContactRouter : ContactModule.IRouter {
    val sendEmailLiveEvent = SingleLiveEvent<String>()
    val openTelegramGroupEvent = SingleLiveEvent<String>()

    override fun openSendMail(recipient: String) {
        sendEmailLiveEvent.postValue(recipient)
    }

    override fun openTelegram(group: String) {
        openTelegramGroupEvent.postValue(group)
    }

}
