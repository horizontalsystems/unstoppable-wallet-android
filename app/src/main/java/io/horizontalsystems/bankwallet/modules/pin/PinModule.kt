package io.horizontalsystems.bankwallet.modules.pin

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import java.util.*

object PinModule {

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
        fun showFingerprintDialog(cryptoObject: FingerprintManagerCompat.CryptoObject)
        fun showLockView(until: Date)
        fun showAttemptsLeft(attempts: Int?, pageIndex: Int)
    }

    interface IPinViewDelegate {
        fun viewDidLoad()
        fun onEnter(pin: String, pageIndex: Int)
        fun onDelete(pageIndex: Int)
        fun onBackPressed()
        fun onBiometricUnlock() {}
        fun showBiometricUnlock() {}
        fun resetPin()
    }

    interface IPinInteractor {
        fun set(pin: String?)
        fun validate(pin: String): Boolean
        fun save(pin: String)
        fun unlock(pin: String): Boolean
        fun startAdapters()
    }

    interface IPinInteractorDelegate {
        fun didSavePin()
        fun didFailToSavePin()
        fun didStartedAdapters()
    }

    fun startForSetPin(context: AppCompatActivity, requestCode: Int) {
        PinActivity.startForResult(context, requestCode, PinInteractionType.SET_PIN)
    }

    fun startForEditPin(context: Context) {
        PinActivity.start(context, PinInteractionType.EDIT_PIN)
    }

    fun startForUnlock(context: AppCompatActivity, requestCode: Int, showCancel: Boolean = false) {
        PinActivity.startForResult(context, requestCode, PinInteractionType.UNLOCK, showCancel)
    }

}

enum class PinInteractionType {
    SET_PIN,
    UNLOCK,
    EDIT_PIN
}
