package io.horizontalsystems.bankwallet.modules.info

import androidx.lifecycle.ViewModel

class InfoPresenter(
        val view: InfoModule.IView,
        val router: InfoModule.IRouter
) : ViewModel(), InfoModule.IViewDelegate {

    // IViewDelegate

    override fun onLoad(infoParameters: InfoModule.InfoParameters) {
        view.set(infoParameters.title, infoParameters.description)
    }

    override fun onClickClose() {
        router.goBack()
    }
}
