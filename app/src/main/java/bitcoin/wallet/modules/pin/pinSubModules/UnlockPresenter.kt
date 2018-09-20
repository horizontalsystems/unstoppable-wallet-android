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

    override fun onEnterDigit(digit: Int) {
        super.onEnterDigit(digit)

        if (enteredPin.length == PinModule.pinLength) {
            interactor.submit(enteredPin.toString())
        }
    }

}
