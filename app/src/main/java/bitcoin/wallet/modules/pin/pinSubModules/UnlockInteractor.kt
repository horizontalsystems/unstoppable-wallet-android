package bitcoin.wallet.modules.pin.pinSubModules

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.ISettingsManager
import bitcoin.wallet.modules.pin.PinInteractor

class UnlockInteractor(private val storage: ILocalStorage, private val settings: ISettingsManager) : PinInteractor() {

    override fun submit(pin: String) {

        if (storage.getPin() == pin) {
            delegate?.onCorrectPinSubmitted()
        } else {
            delegate?.onWrongPinSubmitted()
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        if (settings.isFingerprintEnabled()) {
            delegate?.onFingerprintEnabled()
        }
    }

    override fun onBackPressed() {
        delegate?.onMinimizeApp()
    }

}
