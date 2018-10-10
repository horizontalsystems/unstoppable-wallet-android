package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.modules.pin.PinInteractor
import bitcoin.wallet.modules.pin.PinModule

class SetPinConfirmationInteractor(private val enteredPin: String, private val storage: ILocalStorage, private val keystoreSafeExecute: PinModule.IKeyStoreSafeExecute) : PinInteractor() {
    override fun submit(pin: String) {

        if (pin == enteredPin) {
            keystoreSafeExecute.safeExecute(
                    action = Runnable { storage.savePin(pin) },
                    onSuccess = Runnable {  delegate?.onDidPinSet() },
                    onFailure = Runnable { delegate?.onErrorFailedToSavePin() }
            )
//            try {
//                storage.savePin(pin)
//            } catch (e: Exception) {
//                delegate?.onErrorFailedToSavePin()
//                return
//            }
//            delegate?.onDidPinSet()
        } else {
            delegate?.onErrorPinsDontMatch()
        }
    }

}