package io.horizontalsystems.bankwallet.modules.pin.set

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.ManagePinPresenter
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.pin.PinPage

class SetPinPresenter(
        interactor: PinModule.IPinInteractor,
        private val router: SetPinModule.ISetPinRouter): ManagePinPresenter(interactor, pages = listOf(Page.ENTER, Page.CONFIRM)) {

    override fun viewDidLoad() {
        view?.setTitle(R.string.set_pin_title)

        val pinPages = mutableListOf<PinPage>()
        pages.forEach { page ->
            when(page) {
                Page.ENTER -> pinPages.add(PinPage(R.string.set_pin_description))
                Page.CONFIRM -> pinPages.add(PinPage(R.string.set_pin_confirm_title))
            }
        }
        view?.addPages(pinPages)
    }

    override fun didSavePin() {
        router.navigateToMain()
    }

}
