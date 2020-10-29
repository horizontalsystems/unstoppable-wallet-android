package io.horizontalsystems.pin.edit

import io.horizontalsystems.pin.*

class EditPinPresenter(
        override val view: PinModule.IView,
        val router: EditPinModule.IRouter,
        interactor: PinModule.IInteractor)
    : ManagePinPresenter(view, interactor, pages = listOf(Page.UNLOCK, Page.ENTER, Page.CONFIRM)) {

    override fun viewDidLoad() {
        view.setToolbar(R.string.EditPin_Title)
        view.enablePinInput()
        val pinPages = mutableListOf<PinPage>()

        pages.forEach { page ->
            when (page) {
                Page.UNLOCK -> pinPages.add(PinPage(TopText.Description(R.string.EditPin_UnlockInfo)))
                Page.ENTER -> pinPages.add(PinPage(TopText.Description(R.string.EditPin_NewPinInfo)))
                Page.CONFIRM -> pinPages.add(PinPage(TopText.Description(R.string.SetPin_ConfirmInfo)))
            }
        }
        view.addPages(pinPages)
    }

    override fun didSavePin() {
        router.dismissModuleWithSuccess()
    }

}
