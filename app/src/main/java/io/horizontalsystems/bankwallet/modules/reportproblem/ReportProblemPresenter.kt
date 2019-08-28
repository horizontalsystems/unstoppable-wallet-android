package io.horizontalsystems.bankwallet.modules.reportproblem

import androidx.lifecycle.ViewModel

class ReportProblemPresenter(
        private val interactor: ReportProblemModule.IInteractor,
        val router: ReportProblemModule.IRouter
) : ViewModel(), ReportProblemModule.IInteractorDelegate, ReportProblemModule.IViewDelegate {

    override fun didTapEmail() {
        router.openSendMail(interactor.email)
    }

    override fun didTapTelegram() {
        router.openTelegram(interactor.telegramGroup)
    }

}
