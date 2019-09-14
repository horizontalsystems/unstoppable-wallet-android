package io.horizontalsystems.bankwallet.modules.pin.main

class PinContainerPresenter(
        private val router: PinContainerModule.IRouter,
        private val showCancelButton: Boolean) : PinContainerModule.IViewDelegate {

    var view: PinContainerModule.IView? = null

    override fun onBackPressed() {
        if (showCancelButton) {
            router.closeActivity()
        } else {
            router.closeApplication()
        }
    }

}
