package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.ISecuredStorage
import bitcoin.wallet.modules.pin.PinInteractor

class EditPinAuthInteractor(private val secureStorage: ISecuredStorage, private val keystoreSafeExecute: IKeyStoreSafeExecute) : PinInteractor() {

    override fun submit(pin: String) {
        keystoreSafeExecute.safeExecute(
                action = Runnable {
                    if (secureStorage.savedPin == pin) {
                        delegate?.goToPinEdit()
                    } else {
                        delegate?.onWrongPinSubmitted()
                    }
                }
        )
    }

}
