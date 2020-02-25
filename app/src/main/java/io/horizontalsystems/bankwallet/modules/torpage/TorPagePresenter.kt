package io.horizontalsystems.bankwallet.modules.torpage

import androidx.lifecycle.ViewModel

class TorPagePresenter(
        val view: TorPageModule.IView,
        val router: TorPageModule.IRouter,
        private val interactor: TorPageModule.IInteractor
) : ViewModel(), TorPageModule.IViewDelegate {

    override fun viewDidLoad() {
        view.setTorSwitch(interactor.isTorEnabled)
    }

    override fun onClose() {
        router.close()
    }

    override fun onTorSwitch(enabled: Boolean) {
        interactor.isTorEnabled = enabled
        if (enabled) {
            interactor.enableTor()
        } else {
            interactor.disableTor()
        }
    }

}
