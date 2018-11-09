package io.horizontalsystems.bankwallet.modules.pin

import io.horizontalsystems.bankwallet.R

open class ManagePinPresenter(
        private val interactor: PinModule.IPinInteractor,
        val pages: List<Page>) : PinModule.IPinViewDelegate, PinModule.IPinInteractorDelegate {

    enum class Page { UNLOCK, ENTER, CONFIRM }

    var view: PinModule.IPinView? = null
    private var enteredPin = ""


    override fun viewDidLoad() {
    }

    override fun onEnter(pin: String, pageIndex: Int) {
        if (enteredPin.length < PinModule.PIN_COUNT) {
            enteredPin += pin
            view?.fillCircles(enteredPin.length, pageIndex)

            if (enteredPin.length == PinModule.PIN_COUNT) {
                navigateToPage(pageIndex, enteredPin)
                enteredPin = ""
            }
        }
    }

    private fun navigateToPage(pageIndex: Int, pin: String) {
        when (pages[pageIndex]) {
            Page.UNLOCK -> onEnterFromUnlock(pin)
            Page.ENTER -> onEnterFromEnterPage(pin)
            Page.CONFIRM -> onEnterFromConfirmPage(pin)
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

    override fun onCancel() {
    }

    override fun didSavePin() {
    }

    override fun didStartedAdapters() {
    }

    override fun didFailToSavePin() {
        showEnterPage()
        view?.showError(R.string.set_pin_error_failed_to_save_pin)
    }

    private fun show(page: Page) {
        val pageIndex = pages.indexOfFirst { it == page }
        if (pageIndex >= 0) {
            view?.showPage(pageIndex)
        }
    }

    private fun show(error: Int, page: Page) {
        val pageIndex = pages.indexOfFirst { it == page }
        if (pageIndex >= 0) {
            view?.showErrorForPage(error, pageIndex)
        }
    }

    private fun showEnterPage() {
        interactor.set(null)
        show(Page.ENTER)
    }

    private fun onEnterFromUnlock(pin: String) {
        if (interactor.unlock(pin)) {
            show(Page.ENTER)
        } else {
            val pageUnlockIndex = pages.indexOfFirst { it == Page.UNLOCK }
            if (pageUnlockIndex >= 0) {
                enteredPin = ""
                view?.showPinWrong(pageUnlockIndex)
            }
        }
    }

    private fun onEnterFromEnterPage(pin: String) {
        interactor.set(pin)
        show(Page.CONFIRM)
    }

    private fun onEnterFromConfirmPage(pin: String) {
        if (interactor.validate(pin)) {
            interactor.save(pin)
        } else {
            showEnterPage()
            show(R.string.set_pin_error_pins_dont_match, page = Page.ENTER)
        }
    }
}
