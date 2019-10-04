package io.horizontalsystems.bankwallet.modules.reportproblem

import io.horizontalsystems.bankwallet.SingleLiveEvent

class ReportProblemRouter : ReportProblemModule.IRouter {
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
