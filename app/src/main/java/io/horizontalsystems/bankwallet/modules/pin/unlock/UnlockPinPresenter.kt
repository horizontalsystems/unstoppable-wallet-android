package io.horizontalsystems.bankwallet.modules.pin.unlock

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.LockoutState
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.pin.PinPage

class UnlockPinPresenter(
        private val interactor: UnlockPinModule.IUnlockPinInteractor,
        private val router: UnlockPinModule.IUnlockPinRouter,
        private val showCancelButton: Boolean) : PinModule.IPinViewDelegate, UnlockPinModule.IUnlockPinInteractorDelegate {

    private val unlockPageIndex = 0
    private var enteredPin = ""
    var view: PinModule.IPinView? = null

    override fun viewDidLoad() {
        view?.addPages(listOf(PinPage(R.string.Unlock_Page_EnterYourPin)))

        if (showCancelButton) {
            view?.showBackButton()
        } else {
            view?.hideToolbar()
        }

        interactor.updateLockoutState()

        showFingerprintUnlock()
    }

    override fun onEnter(pin: String, pageIndex: Int) {
        if (enteredPin.length < PinModule.PIN_COUNT) {
            enteredPin += pin
            view?.fillCircles(enteredPin.length, pageIndex)

            if (enteredPin.length == PinModule.PIN_COUNT) {
                if (interactor.unlock(enteredPin)) {
                    router.dismissModuleWithSuccess()
                } else {
                    wrongPinSubmitted()
                }
                enteredPin = ""
            }
        }
    }

    override fun resetPin() {
        enteredPin = ""
    }

    override fun onDelete(pageIndex: Int) {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.substring(0, enteredPin.length - 1)
            view?.fillCircles(enteredPin.length, pageIndex)
        }
    }

    override fun unlock() {
        router.dismissModuleWithSuccess()
    }

    override fun wrongPinSubmitted() {
        view?.showPinWrong(unlockPageIndex)
    }

    override fun showFingerprintUnlock() {
        if (interactor.isFingerprintEnabled && interactor.biometricAuthSupported) {
            interactor.cryptoObject?.let { view?.showFingerprintDialog(it) }
        }
    }

    override fun onFingerprintUnlock() {
        interactor.onUnlock()
    }

    override fun updateLockoutState(state: LockoutState) {
        when (state) {
            is LockoutState.Unlocked -> view?.showAttemptsLeft(state.attemptsLeft, unlockPageIndex)
            is LockoutState.Locked -> view?.showLockView(state.until)
        }
    }

    override fun onBackPressed() {
        if (showCancelButton) {
            router.dismissModuleWithCancel()
        } else {
            router.closeApplication()
        }
    }

}
