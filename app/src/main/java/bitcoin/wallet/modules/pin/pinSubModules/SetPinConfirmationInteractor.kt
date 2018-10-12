package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.modules.pin.PinInteractor

class SetPinConfirmationInteractor(private val enteredPin: String, private val storage: ILocalStorage, private val keystoreSafeExecute: IKeyStoreSafeExecute) : PinInteractor() {
    override fun submit(pin: String) {

        if (pin == enteredPin) {
            keystoreSafeExecute.safeExecute(
                    action = Runnable { storage.savePin(pin) },
                    onSuccess = Runnable {  delegate?.onDidPinSet() },
                    onFailure = Runnable { delegate?.onErrorFailedToSavePin() }
            )
        } else {
            delegate?.onErrorPinsDontMatch()
        }
    }

}