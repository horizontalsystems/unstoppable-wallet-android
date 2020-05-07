package io.horizontalsystems.pin

import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import kotlinx.android.parcel.Parcelize
import java.util.*

object PinModule {

    const val RESULT_OK = 1
    const val RESULT_CANCELLED = 2
    const val PIN_COUNT = 6

    const val keyInteractionType = "interaction_type"
    const val keyShowCancel = "show_cancel"

    interface IView {
        fun setToolbar(title: Int, showBackButton: Boolean)
        fun addPages(pages: List<PinPage>)
        fun showPage(index: Int)
        fun updateTopTextForPage(text: TopText, pageIndex: Int)
        fun showError(error: Int)
        fun showPinWrong(pageIndex: Int)
        fun showCancelButton()
        fun fillCircles(length: Int, pageIndex: Int)
        fun showFingerprintDialog(cryptoObject: BiometricPrompt.CryptoObject)
        fun showLockView(until: Date)
        fun enablePinInput()
        fun showFingerprintButton()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onEnter(pin: String, pageIndex: Int)
        fun onDelete(pageIndex: Int)
        fun onFingerprintUnlock() {}
        fun showFingerprintButton() {}
        fun showFingerprintInput() {}
        fun resetPin()
    }

    interface IInteractor {
        fun set(pin: String?)
        fun validate(pin: String): Boolean
        fun save(pin: String)
        fun unlock(pin: String): Boolean
    }

    interface IInteractorDelegate {
        fun didSavePin()
        fun didFailToSavePin()
    }

    fun startForSetPin(context: AppCompatActivity, requestCode: Int) {
        PinActivity.startForResult(context, PinInteractionType.SET_PIN, requestCode)
    }

    fun startForEditPin(context: AppCompatActivity) {
        PinActivity.startForResult(context, PinInteractionType.EDIT_PIN)
    }

    fun startForUnlock(context: AppCompatActivity, requestCode: Int) {
        PinActivity.startForResult(context, PinInteractionType.UNLOCK, requestCode, true)
    }

}

sealed class TopText(open val text: Int){
    class Title(override val text: Int): TopText(text)
    class BigError(override val text: Int): TopText(text)
    class Description(override val text: Int): TopText(text)
    class SmallError(override val text: Int): TopText(text)
}

@Parcelize
enum class PinInteractionType: Parcelable {
    SET_PIN,
    UNLOCK,
    EDIT_PIN
}
