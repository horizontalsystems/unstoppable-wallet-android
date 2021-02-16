package io.horizontalsystems.pin.unlock

import androidx.lifecycle.ViewModel
import io.horizontalsystems.pin.PinModule
import io.horizontalsystems.pin.PinPage
import io.horizontalsystems.pin.R
import io.horizontalsystems.pin.TopText
import io.horizontalsystems.pin.core.LockoutState

class UnlockPinPresenter(
        val view: PinModule.IView,
        val router: UnlockPinModule.IRouter,
        private val interactor: UnlockPinModule.IInteractor,
        private val showCancelButton: Boolean)
    : ViewModel(), PinModule.IViewDelegate, UnlockPinModule.IInteractorDelegate {

    private val unlockPageIndex = 0
    private var enteredPin = ""
    private var isShowingPinMismatchError = false

    override fun viewDidLoad() {
        view.addPages(listOf(PinPage(TopText.Title(R.string.Unlock_Page_EnterYourPin))))

        if (showCancelButton) {
            view.showCancelButton()
        }

        interactor.updateLockoutState()

        showBiometricAuthButton()
        showBiometricAuthInput()
    }

    override fun onEnter(pin: String, pageIndex: Int) {
        if (pageIndex >= 0 && enteredPin.length < PinModule.PIN_COUNT) {
            enteredPin += pin
            removeErrorMessage()
            view.fillCircles(enteredPin.length, pageIndex)

            if (enteredPin.length == PinModule.PIN_COUNT) {
                if (interactor.unlock(enteredPin)) {
                    router.dismissModuleWithSuccess()
                } else {
                    wrongPinSubmitted()
                }
            }
        }
    }

    override fun resetPin() {
        enteredPin = ""
    }

    override fun onDelete(pageIndex: Int) {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.substring(0, enteredPin.length - 1)
            view.fillCircles(enteredPin.length, pageIndex)
        }
    }

    override fun unlock() {
        router.dismissModuleWithSuccess()
    }

    override fun wrongPinSubmitted() {
        view.showPinWrong(unlockPageIndex)
        isShowingPinMismatchError = true
        view.updateTopTextForPage(TopText.BigError(R.string.UnlockPin_Error_PinIncorrect), unlockPageIndex)
    }

    override fun showBiometricAuthButton() {
        if (interactor.isBiometricAuthEnabled && interactor.isBiometricAuthSupported) {
            view.showBiometricAuthButton()
        }
    }

    override fun showBiometricAuthInput() {
        if(interactor.isBiometricAuthEnabled) {
            view.showBiometricAuthDialog()
        }
    }

    override fun onBiometricsUnlock() {
        interactor.onUnlock()
    }

    override fun updateLockoutState(state: LockoutState) {
        when (state) {
            is LockoutState.Unlocked -> view.enablePinInput()
            is LockoutState.Locked -> view.showLockView(state.until)
        }
    }

    private fun removeErrorMessage() {
        if (isShowingPinMismatchError && enteredPin.isNotEmpty()) {
            view.updateTopTextForPage(TopText.Title(R.string.Unlock_Page_EnterYourPin), unlockPageIndex)
            isShowingPinMismatchError = false
        }
    }

}
