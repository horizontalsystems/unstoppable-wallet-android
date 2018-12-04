package io.horizontalsystems.bankwallet.modules.pin.edit

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.ManagePinPresenter
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.pin.PinPage

class EditPinPresenter(
        interactor: PinModule.IPinInteractor,
        private val router: EditPinModule.IEditPinRouter): ManagePinPresenter(interactor, pages = listOf(Page.UNLOCK, Page.ENTER, Page.CONFIRM)) {

    override fun viewDidLoad() {
        view?.setTitle(R.string.EditPin_Title)
        val pinPages = mutableListOf<PinPage>()

        pages.forEach { page ->
            when(page) {
                Page.UNLOCK -> pinPages.add(PinPage(R.string.EditPin_UnlockInfo))
                Page.ENTER -> pinPages.add(PinPage(R.string.EditPin_NewPinInfo))
                Page.CONFIRM -> pinPages.add(PinPage(R.string.SetPin_ConfirmInfo))
            }
        }
        view?.addPages(pinPages)
        view?.showCancel()
    }

    override fun onCancel() {
        router.dismiss()
    }

    override fun didSavePin() {
        router.dismiss()
    }

    override fun onBackPressed() {
        router.dismiss()
    }
}
