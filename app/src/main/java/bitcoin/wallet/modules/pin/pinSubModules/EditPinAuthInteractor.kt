package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.modules.pin.PinInteractor

class EditPinAuthInteractor(private val storage: ILocalStorage, private val keystoreSafeExecute: IKeyStoreSafeExecute) : PinInteractor() {

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
