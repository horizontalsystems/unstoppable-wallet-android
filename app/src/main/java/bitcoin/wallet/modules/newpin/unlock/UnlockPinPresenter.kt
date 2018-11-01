package bitcoin.wallet.modules.newpin.unlock

import android.os.Handler
import bitcoin.wallet.R
import bitcoin.wallet.modules.newpin.PinModule
import bitcoin.wallet.modules.newpin.PinPage

class UnlockPinPresenter(
        private val interactor: UnlockPinModule.IUnlockPinInteractor,
        private val router: UnlockPinModule.IUnlockPinRouter): PinModule.IPinViewDelegate, UnlockPinModule.IUnlockPinInteractorDelegate {

    private var enteredPin = ""
    var view: PinModule.IPinView? = null

    override fun viewDidLoad() {
        view?.hideToolbar()
        view?.addPages(listOf(PinPage(R.string.unlock_page_enter_your_pin)))
        interactor.biometricUnlock()
    }

    override fun onEnter(pin: String, pageIndex: Int) {
        if (enteredPin.length < PinModule.PIN_COUNT) {
            enteredPin += pin
            view?.fillCircles(enteredPin.length, pageIndex)

            if (enteredPin.length == PinModule.PIN_COUNT) {
                interactor.unlock(enteredPin)
            }
        }
    }

    override fun onCancel() {
        router.dismiss(false)
    }

    override fun onDelete(pageIndex: Int) {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.substring(0, enteredPin.length - 1)
            view?.fillCircles(enteredPin.length, pageIndex)
        }
    }

    override fun didBiometricUnlock() {
        router.dismiss(true)
    }

    override fun unlock() {
        router.dismiss(true)
    }

    override fun showFingerprintInput() {
        view?.showFingerprintDialog()
    }

    override fun wrongPinSubmitted() {
        view?.showPinWrong(0)
        Handler().postDelayed({
            enteredPin = ""
            view?.fillCircles(enteredPin.length, 0)
        }, 500)
    }

    override fun onBiometricUnlock() {
        interactor.onUnlock()
    }

    override fun showBiometricUnlock() {
        interactor.biometricUnlock()
    }
}
