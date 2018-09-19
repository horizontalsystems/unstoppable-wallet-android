package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.modules.pin.PinModule
import bitcoin.wallet.modules.pin.PinPresenter

class SetPinPresenter(interactor: PinModule.IInteractor, router: PinModule.IRouter) : PinPresenter(interactor, router) {

    override fun updateViewTitleAndDescription() {
        view?.setTitleEnterPin()
        view?.setDescriptionEnterPin()
    }
}
