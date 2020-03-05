package io.horizontalsystems.bankwallet.modules.torpage

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.TorStatus

class TorPagePresenter(
        val view: TorPageModule.IView,
        val router: TorPageModule.IRouter,
        private val interactor: TorPageModule.IInteractor
) : ViewModel(), TorPageModule.IViewDelegate, TorPageModule.InteractorDelegate {

    override fun viewDidLoad() {
        view.setTorSwitch(interactor.isTorEnabled)
        interactor.onViewLoad()
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

    override fun updateConnectionStatus(connectionStatus: TorStatus) {
        view.setConnectionStatus(connectionStatus)
        if (connectionStatus == TorStatus.Failed){
            interactor.isTorEnabled = false
            view.setTorSwitch(false)
        }
    }

    override fun onCleared() {
        interactor.onClear()
    }
}
