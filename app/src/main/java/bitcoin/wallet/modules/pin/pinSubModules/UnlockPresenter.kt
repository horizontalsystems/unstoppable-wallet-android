package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.modules.pin.PinModule
import bitcoin.wallet.modules.pin.PinPresenter

class UnlockPresenter(interactor: PinModule.IInteractor, router: PinModule.IRouter) : PinPresenter(interactor, router) {

    override fun viewDidLoad() {
        super.viewDidLoad()

        interactor.viewDidLoad()
    }

    override fun updateViewTitleAndDescription() {
        view?.hideToolbar()
        view?.setDescriptionForUnlock()
    }

}
