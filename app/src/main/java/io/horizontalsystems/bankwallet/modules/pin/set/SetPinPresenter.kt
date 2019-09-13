package io.horizontalsystems.bankwallet.modules.pin.set

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.ManagePinPresenter
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.pin.PinPage
import io.horizontalsystems.bankwallet.modules.pin.TopText

class SetPinPresenter(
        private val interactor: PinModule.IPinInteractor,
        private val router: SetPinModule.ISetPinRouter) : ManagePinPresenter(interactor, pages = listOf(Page.ENTER, Page.CONFIRM)) {

    override fun viewDidLoad() {
        view?.setTitle(R.string.SetPin_Title)

        val pinPages = mutableListOf<PinPage>()
        pages.forEach { page ->
            when (page) {
                Page.ENTER -> pinPages.add(PinPage(TopText.Description(R.string.SetPin_Info)))
                Page.CONFIRM -> pinPages.add(PinPage(TopText.Description(R.string.SetPin_ConfirmInfo)))
            }
        }
        view?.addPages(pinPages)
    }

    override fun onBackPressed() {
        router.dismissModuleWithCancel()
    }

    override fun didSavePin() {
        router.dismissModuleWithSuccess()
    }

}
