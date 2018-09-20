package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.modules.pin.PinModule
import bitcoin.wallet.modules.pin.PinPresenter

class EditPinAuthPresenter(interactor: PinModule.IInteractor, router: PinModule.IRouter) : PinPresenter(interactor, router) {

    override fun updateViewTitleAndDescription() {
        view?.setTitleForEditPinAuth()
        view?.setDescriptionForEditPinAuth()
    }

}
