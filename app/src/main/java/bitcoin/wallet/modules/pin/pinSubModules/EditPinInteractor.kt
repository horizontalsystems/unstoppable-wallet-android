package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.ISecuredStorage
import bitcoin.wallet.modules.pin.PinInteractor
import bitcoin.wallet.modules.pin.PinModule

class EditPinInteractor(private val securedStorage: ISecuredStorage, private val keystoreSafeExecute: IKeyStoreSafeExecute) : PinInteractor() {

    override fun submit(pin: String) {

        if (pin.length == PinModule.pinLength) {
            keystoreSafeExecute.safeExecute(
                    action = Runnable { securedStorage.savePin(pin) },
                    onSuccess = Runnable { delegate?.onDidPinSet() },
                    onFailure = Runnable { delegate?.onErrorFailedToSavePin() }
            )
        } else {
            delegate?.onErrorShortPinLength()
        }
    }
}
