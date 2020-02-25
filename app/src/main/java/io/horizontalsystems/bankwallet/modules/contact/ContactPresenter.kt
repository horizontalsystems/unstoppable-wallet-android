package io.horizontalsystems.bankwallet.modules.contact

import androidx.lifecycle.ViewModel

class ContactPresenter(
        val view: ContactModule.IView,
        val router: ContactModule.IRouter,
        private val interactor: ContactModule.IInteractor
) : ViewModel(), ContactModule.IViewDelegate, ContactModule.IRouterDelegate {

    override fun viewDidLoad() {
        view.setEmail(interactor.email)
        view.setWalletHelpTelegramGroup("@${interactor.walletHelpTelegramGroup}")
        view.setDevelopersTelegramGroup("@${interactor.developersTelegramGroup}")
    }

    override fun didTapEmail() {
        router.openSendMail(interactor.email)
    }

    override fun didTapWalletHelpTelegram() {
        router.openTelegram(interactor.walletHelpTelegramGroup)
    }

    override fun didTapDevelopersTelegram() {
        router.openTelegram(interactor.developersTelegramGroup)
    }

    override fun didTapAppStatus() {
        router.openAppStatus()
    }

    override fun didFailSendMail() {
        interactor.copyToClipboard(interactor.email)
        view.showCopied()
    }
}
