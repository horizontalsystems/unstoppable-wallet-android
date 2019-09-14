package io.horizontalsystems.bankwallet.modules.pin.main

import androidx.lifecycle.ViewModel

class PinContainerPresenter(
        val router: PinContainerModule.Router,
        private val showCancelButton: Boolean) : ViewModel(), PinContainerModule.ViewDelegate {

    override fun onBackPressed() {
        if (showCancelButton) {
            router.closeActivity()
        } else {
            router.closeApplication()
        }
    }

}
