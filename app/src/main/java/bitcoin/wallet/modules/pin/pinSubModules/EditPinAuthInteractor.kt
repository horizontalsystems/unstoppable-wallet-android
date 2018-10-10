package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.modules.pin.PinInteractor
import bitcoin.wallet.modules.pin.PinModule

class EditPinAuthInteractor(private val storage: ILocalStorage, private val keystoreSafeExecute: PinModule.IKeyStoreSafeExecute) : PinInteractor() {

    override fun submit(pin: String) {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    if (storage.getPin() == pin) {
                        delegate?.goToPinEdit()
                    } else {
                        delegate?.onWrongPinSubmitted()
                    }
                }
        )
    }

}
