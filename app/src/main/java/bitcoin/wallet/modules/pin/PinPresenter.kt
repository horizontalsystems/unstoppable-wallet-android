package bitcoin.wallet.modules.pin


abstract class PinPresenter(protected val interactor: PinModule.IInteractor, protected val router: PinModule.IRouter) : PinModule.IViewDelegate, PinModule.IInteractorDelegate {
    var view: PinModule.IView? = null

    private var enteredPin: StringBuilder = StringBuilder()

    override fun viewDidLoad() {
        updateViewTitleAndDescription()
    }

    abstract fun updateViewTitleAndDescription()

    override fun onEnterDigit(digit: Int) {
        if (enteredPin.length < PinModule.pinLength) {
            enteredPin.append(digit)
        }

        view?.highlightPinMask(enteredPin.length)
    }

    override fun onClickDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.lastIndex)
        }

        view?.highlightPinMask(enteredPin.length)
    }

    override fun onClickDone() {
        interactor.submit(enteredPin.toString())
    }

    //InteractorDelegate

    override fun onErrorShortPinLength() {
        view?.showErrorShortPinLength()
    }

    override fun goToPinConfirmation(pin: String) {
        router.goToPinConfirmation(pin)
    }

    override fun onDidPinSet() {
        view?.showSuccessPinSet()
    }

    override fun onErrorPinsDontMatch() {
        view?.showErrorPinsDontMatch()
    }

    override fun onErrorFailedToSavePin() {
        view?.showErrorFailedToSavePin()
    }
}
