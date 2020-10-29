package io.horizontalsystems.pin.set

import io.horizontalsystems.pin.*

class SetPinPresenter(
        override val view: PinModule.IView,
        val router: SetPinModule.IRouter,
        interactor: PinModule.IInteractor) : ManagePinPresenter(view, interactor, pages = listOf(Page.ENTER, Page.CONFIRM)) {

    override fun viewDidLoad() {
        view.setToolbar(R.string.SetPin_Title)
        view.enablePinInput()

        val pinPages = mutableListOf<PinPage>()
        pages.forEach { page ->
            when (page) {
                Page.ENTER -> pinPages.add(PinPage(TopText.Description(R.string.SetPin_Info)))
                Page.CONFIRM -> pinPages.add(PinPage(TopText.Description(R.string.SetPin_ConfirmInfo)))
                Page.UNLOCK -> {}
            }
        }
        view.addPages(pinPages)
    }

    override fun didSavePin() {
        router.dismissModuleWithSuccess()
    }

}
