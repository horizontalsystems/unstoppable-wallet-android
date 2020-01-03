package io.horizontalsystems.bankwallet.modules.info

import androidx.lifecycle.ViewModel

class InfoPresenter(
        val interactor: InfoModule.IInteractor,
        val view: InfoModule.IView,
        val router: InfoModule.IRouter
) : ViewModel(), InfoModule.IViewDelegate {

    // IViewDelegate

    override fun onLoad(infoParameters: InfoModule.InfoParameters) {
        view.set(infoParameters.title, infoParameters.description)

        infoParameters.txHash?.let { view.setTxHash(it) }
        infoParameters.conflictingTxHash?.let { view.setConflictingTxHash(it) }
    }

    override fun onClickClose() {
        router.goBack()
    }

    override fun onClickTxHash(txHash: String) {
        interactor.onCopy(txHash)
        view.showCopied()
    }

}
