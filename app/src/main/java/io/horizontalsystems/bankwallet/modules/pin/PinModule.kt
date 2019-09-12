package io.horizontalsystems.bankwallet.modules.pin

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import java.util.*

object PinModule {

    const val RESULT_OK = 1
    const val RESULT_CANCELLED = 2

    const val PIN_COUNT = 6

    interface IPinView {
        fun setTitle(title: Int)
        fun addPages(pages: List<PinPage>)
        fun showPage(index: Int)
        fun showErrorForPage(error: Int, pageIndex: Int)
        fun showError(error: Int)
        fun showPinWrong(pageIndex: Int)
        fun showBackButton()
        fun fillCircles(length: Int, pageIndex: Int)
        fun hideToolbar()
        fun showFingerprintDialog(cryptoObject: BiometricPrompt.CryptoObject)
        fun showLockView(until: Date)
        fun showAttemptsLeft(attempts: Int?, pageIndex: Int)
    }

    interface IPinViewDelegate {
        fun viewDidLoad()
        fun onEnter(pin: String, pageIndex: Int)
        fun onDelete(pageIndex: Int)
        fun onBackPressed()
        fun onFingerprintUnlock() {}
        fun showFingerprintUnlock() {}
        fun resetPin()
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

    fun startForSetPin(context: AppCompatActivity, requestCode: Int) {
        PinActivity.startForResult(context, PinInteractionType.SET_PIN, requestCode)
    }

    fun startForEditPin(context: AppCompatActivity) {
        PinActivity.startForResult(context, PinInteractionType.EDIT_PIN)
    }

    fun startForUnlock(context: AppCompatActivity, requestCode: Int, showCancel: Boolean = false) {
        PinActivity.startForResult(context, PinInteractionType.UNLOCK, requestCode, showCancel)
    }

}

enum class PinInteractionType {
    SET_PIN,
    UNLOCK,
    EDIT_PIN
}
