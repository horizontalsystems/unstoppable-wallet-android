package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.modules.pin.PinInteractor
import bitcoin.wallet.modules.pin.PinModule

class EditPinInteractor(private val storage: ILocalStorage) : PinInteractor() {
    override fun submit(pin: String) {

        if (pin.length == PinModule.pinLength) {
            try {
                storage.savePin(pin)
            } catch (e: Exception) {
                delegate?.onErrorFailedToSavePin()
                return
            }
            delegate?.onDidPinSet()

        } else {
            delegate?.onErrorShortPinLength()
        }
    }

}
