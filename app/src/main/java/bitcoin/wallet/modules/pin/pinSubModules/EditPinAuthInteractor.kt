package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.modules.pin.PinInteractor

class EditPinAuthInteractor(private val storage: ILocalStorage) : PinInteractor() {

    override fun submit(pin: String) {
        if (storage.getPin() == pin) {
            delegate?.goToPinEdit()
        } else {
            delegate?.onWrongPinSubmitted()
        }
    }

}
