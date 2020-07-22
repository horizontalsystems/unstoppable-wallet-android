package io.horizontalsystems.bankwallet.modules.settings.appstatus

import androidx.lifecycle.ViewModel

class AppStatusPresenter(
        val view: AppStatusModule.IView,
        private val interactor: AppStatusModule.IInteractor
) : ViewModel(), AppStatusModule.IViewDelegate {

    override fun viewDidLoad() {
        view.setAppStatus(interactor.status)
    }

    override fun didTapCopy(text: String) {
        interactor.copyToClipboard(text)
        view.showCopied()
    }

}
