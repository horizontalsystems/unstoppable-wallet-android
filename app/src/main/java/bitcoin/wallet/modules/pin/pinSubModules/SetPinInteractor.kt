package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.modules.pin.PinInteractor
import bitcoin.wallet.modules.pin.PinModule

class SetPinInteractor : PinInteractor() {

    override fun submit(pin: String) {

        if (pin.length < PinModule.pinLength)
            delegate?.onErrorShortPinLength()
        else
            delegate?.goToPinConfirmation(pin)
    }

    override fun onBackPressed() {
        delegate?.onMinimizeApp()
    }

}
