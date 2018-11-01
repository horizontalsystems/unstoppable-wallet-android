package bitcoin.wallet.modules.newpin.edit

import bitcoin.wallet.R
import bitcoin.wallet.modules.newpin.ManagePinPresenter
import bitcoin.wallet.modules.newpin.PinModule
import bitcoin.wallet.modules.newpin.PinPage

class EditPinPresenter(
        interactor: PinModule.IPinInteractor,
        private val router: EditPinModule.IEditPinRouter): ManagePinPresenter(interactor, pages = listOf(Page.UNLOCK, Page.ENTER, Page.CONFIRM)) {

    override fun viewDidLoad() {
        view?.setTitle(R.string.edit_pin_auth_title)
        val pinPages = mutableListOf<PinPage>()

        pages.forEach { page ->
            when(page) {
                Page.UNLOCK -> pinPages.add(PinPage(R.string.edit_pin_auth_description))
                Page.ENTER -> pinPages.add(PinPage(R.string.edit_pin_title))
                Page.CONFIRM -> pinPages.add(PinPage(R.string.set_pin_confirm_title))
            }
        }
        view?.addPages(pinPages)
        view?.showCancel()
    }

    override fun onCancel() {
        router.dismiss()
    }

    override fun didSavePin() {
        view?.showSuccess()
        router.dismiss()
    }

    override fun onBackPressed() {
        router.dismiss()
    }
}
