package bitcoin.wallet.modules.newpin

import android.content.Context

object PinModule {

    const val PIN_COUNT = 6

    interface IPinView {
        fun setTitle(title: Int)
        fun addPages(pages: List<PinPage>)
        fun showPage(index: Int)
        fun showErrorForPage(error: Int, pageIndex: Int)
        fun showError(error: Int)
        fun showPinWrong(pageIndex: Int)
        fun showCancel()
        fun showSuccess()
        fun fillCircles(length: Int, pageIndex: Int)
        fun hideToolbar()
        fun showFingerprintDialog()
    }

    interface IPinViewDelegate {
        fun viewDidLoad()
        fun onEnter(pin: String, pageIndex: Int)
        fun onCancel()
        fun onDelete(pageIndex: Int)
        fun onBackPressed() {}
        fun onBiometricUnlock() {}
        fun showBiometricUnlock() {}
    }

    interface IPinInteractor {
        fun set(pin: String?)
        fun validate(pin: String): Boolean
        fun save(pin: String)

        fun unlock(pin: String): Boolean
    }

    interface IPinInteractorDelegate {
        fun didSavePin()
        fun didFailToSavePin()
    }

    fun startForSetPin(context: Context) {
        NewPinActivity.start(context, NewPinInteractionType.SET_PIN)
    }

    fun startForEditPin(context: Context) {
        NewPinActivity.start(context, NewPinInteractionType.EDIT_PIN)
    }

    fun startForUnlock() {
        NewPinActivity.startForUnlock()
    }
}

enum class NewPinInteractionType {
    SET_PIN,
    UNLOCK,
    EDIT_PIN
}
