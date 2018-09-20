package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.modules.pin.PinInteractor

class SetPinConfirmationInteractor(private val enteredPin: String, private val storage: ILocalStorage) : PinInteractor() {
    override fun submit(pin: String) {

        if (pin == enteredPin) {
            try {
                storage.savePin(pin)
            } catch (e: Exception) {
                delegate?.onErrorFailedToSavePin()
                return
            }
            delegate?.onDidPinSet()
        } else {
            delegate?.onErrorPinsDontMatch()
        }
    }

}