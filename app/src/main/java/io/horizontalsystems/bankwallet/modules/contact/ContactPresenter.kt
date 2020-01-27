package io.horizontalsystems.bankwallet.modules.contact

import androidx.lifecycle.ViewModel

class ContactPresenter(
        val view: ContactModule.IView,
        val router: ContactModule.IRouter,
        private val interactor: ContactModule.IInteractor
) : ViewModel(), ContactModule.IViewDelegate, ContactModule.IRouterDelegate {

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
