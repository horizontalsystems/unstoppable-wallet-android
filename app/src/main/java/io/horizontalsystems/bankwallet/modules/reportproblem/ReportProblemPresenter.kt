package io.horizontalsystems.bankwallet.modules.reportproblem

import androidx.lifecycle.ViewModel

class ReportProblemPresenter(
        val view: ReportProblemModule.IView,
        val router: ReportProblemModule.IRouter,
        private val interactor: ReportProblemModule.IInteractor
) : ViewModel(), ReportProblemModule.IViewDelegate, ReportProblemModule.IRouterDelegate {

    override fun viewDidLoad() {
        view.setEmail(interactor.email)
        view.setTelegramGroup("@" + interactor.telegramGroup)
    }

    override fun didTapEmail() {
        router.openSendMail(interactor.email)
    }

    override fun didTapTelegram() {
        router.openTelegram(interactor.telegramGroup)
    }

    override fun didTapAppStatus() {
        router.openAppStatus()
    }

    override fun didFailSendMail() {
        interactor.copyToClipboard(interactor.email)
        view.showCopied()
    }
}
