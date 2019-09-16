package io.horizontalsystems.bankwallet.modules.pin

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import java.util.*

object PinModule {

    const val RESULT_OK = 1
    const val RESULT_CANCELLED = 2
    const val PIN_COUNT = 6

    interface View {
        fun setTitle(title: Int)
        fun addPages(pages: List<PinPage>)
        fun showPage(index: Int)
        fun updateTopTextForPage(text: TopText, pageIndex: Int)
        fun showError(error: Int)
        fun showPinWrong(pageIndex: Int)
        fun showBackButton()
        fun fillCircles(length: Int, pageIndex: Int)
        fun hideToolbar()
        fun showFingerprintDialog(cryptoObject: BiometricPrompt.CryptoObject)
        fun showLockView(until: Date)
        fun showPinInput()
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onEnter(pin: String, pageIndex: Int)
        fun onDelete(pageIndex: Int)
        fun onFingerprintUnlock() {}
        fun showFingerprintUnlock() {}
        fun resetPin()
    }

    interface Interactor {
        fun set(pin: String?)
        fun validate(pin: String): Boolean
        fun save(pin: String)
        fun unlock(pin: String): Boolean
    }

    interface InteractorDelegate {
        fun didSavePin()
        fun didFailToSavePin()
    }

    fun startForSetPin(context: AppCompatActivity, requestCode: Int) {
        PinActivity.startForResult(context, PinInteractionType.SET_PIN, requestCode)
    }

    fun startForEditPin(context: AppCompatActivity) {
        PinActivity.startForResult(context, PinInteractionType.EDIT_PIN)
    }

    fun startForUnlock(context: AppCompatActivity, requestCode: Int, showCancel: Boolean = false, showRates: Boolean = false) {
        PinActivity.startForResult(context, PinInteractionType.UNLOCK, requestCode, showCancel, showRates)
    }

}

sealed class TopText(open val text: Int){
    class Title(override val text: Int): TopText(text)
    class BigError(override val text: Int): TopText(text)
    class Description(override val text: Int): TopText(text)
    class SmallError(override val text: Int): TopText(text)
}

enum class PinInteractionType {
    SET_PIN,
    UNLOCK,
    EDIT_PIN
}
