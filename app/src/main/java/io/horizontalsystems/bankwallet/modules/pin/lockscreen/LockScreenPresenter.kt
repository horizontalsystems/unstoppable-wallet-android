package io.horizontalsystems.bankwallet.modules.pin.lockscreen

import androidx.lifecycle.ViewModel

class LockScreenPresenter(
        val router: LockScreenModule.IRouter,
        private val showCancelButton: Boolean) : ViewModel(), LockScreenModule.IViewDelegate {

    override fun onBackPressed() {
        if (showCancelButton) {
            router.closeActivity()
        } else {
            router.closeApplication()
        }
    }

}
