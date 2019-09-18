package io.horizontalsystems.bankwallet.modules.pin.lockscreen

import androidx.lifecycle.ViewModel

class LockScreenPresenter(
        val router: LockScreenModule.Router,
        private val showCancelButton: Boolean) : ViewModel(), LockScreenModule.ViewDelegate {

    override fun onBackPressed() {
        if (showCancelButton) {
            router.closeActivity()
        } else {
            router.closeApplication()
        }
    }

}
