package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.modules.pin.PinInteractor

class EditPinInteractor(private val storage: ILocalStorage) : PinInteractor() {
    override fun submit(pin: String) {

        try {
            storage.savePin(pin)
        } catch (e: Exception) {
            delegate?.onErrorFailedToSavePin()
            return
        }
        delegate?.onDidPinSet()
    }

}
