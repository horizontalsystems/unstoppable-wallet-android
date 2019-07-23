package io.horizontalsystems.bankwallet.modules.restore.eos

import io.horizontalsystems.bankwallet.R

class RestoreEosPresenter(private val interactor: RestoreEosModule.IInteractor, private val router: RestoreEosModule.IRouter)
    : RestoreEosModule.IViewDelegate, RestoreEosModule.IInteractorDelegate {

    var view: RestoreEosModule.IView? = null

    //  ViewDelegate

    override fun onClickDone(accountName: String, privateKey: String) {
        interactor.validate(accountName, privateKey)
    }

    override fun onClickScan() {
        router.startQRScanner()
    }

    override fun onPasteAccount() {
        interactor.textFromClipboard?.let { view?.setAccount(it) }
    }

    override fun onPasteKey() {
        interactor.textFromClipboard?.let { view?.setPrivateKey(it) }
    }

    override fun onQRCodeScan(key: String?) {
        key?.let { view?.setPrivateKey(key) }
    }

    //  InteractorDelegate

    override fun onValidationSuccess(accountName: String, privateKey: String) {
        router.finishWithSuccess(accountName, privateKey)
    }

    override fun onInvalidAccount() {
        view?.showError(R.string.Restore_EosAccountIncorrect)
    }

    override fun onInvalidKey() {
        view?.showError(R.string.Restore_EosKeyIncorrect)
    }
}
