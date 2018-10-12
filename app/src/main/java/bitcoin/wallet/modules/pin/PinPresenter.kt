package bitcoin.wallet.modules.pin


abstract class PinPresenter(protected val interactor: PinModule.IInteractor, protected val router: PinModule.IRouter) : PinModule.IViewDelegate, PinModule.IInteractorDelegate {
    var view: PinModule.IView? = null

    protected var enteredPin: StringBuilder = StringBuilder()

    override fun viewDidLoad() {
        updateViewTitleAndDescription()
    }

    abstract fun updateViewTitleAndDescription()

    override fun onEnterDigit(digit: Int) {
        if (enteredPin.length < PinModule.pinLength) {
            enteredPin.append(digit)
        }

        view?.highlightPinMask(enteredPin.length)

        if (enteredPin.length == PinModule.pinLength) {
            interactor.submit(enteredPin.toString())
        }
    }

    override fun onClickDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.lastIndex)
        }

        view?.highlightPinMask(enteredPin.length)
    }

    override fun onBackPressed() {
        interactor.onBackPressed()
    }

    //InteractorDelegate

    override fun onErrorShortPinLength() {
        view?.showErrorShortPinLength()
    }

    override fun goToPinConfirmation(pin: String) {
        router.goToPinConfirmation(pin)
    }

    override fun goToPinEdit() {
        router.goToPinEdit()
    }

    override fun onDidPinSet() {
        view?.showSuccessPinSet()
    }

    override fun onErrorPinsDontMatch() {
        view?.showErrorPinsDontMatch()
        enteredPin.setLength(0)
        view?.clearPinMaskWithDelay()
    }

    override fun onErrorFailedToSavePin() {
        view?.showErrorFailedToSavePin()
    }

    override fun onCorrectPinSubmitted() {
        router.unlockWallet()
    }

    override fun onWrongPinSubmitted() {
        enteredPin.setLength(0)
        view?.showErrorWrongPin()
        view?.clearPinMaskWithDelay()
    }

    override fun onFingerprintEnabled() {
        view?.showFingerprintDialog()
    }

    override fun onMinimizeApp() {
        view?.minimizeApp()
    }

    override fun onNavigateToPrevPage() {
        view?.navigateToPrevPage()
    }
}
