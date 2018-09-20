package bitcoin.wallet.modules.pin

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.R
import bitcoin.wallet.SingleLiveEvent

class PinViewModel : ViewModel(), PinModule.IView, PinModule.IRouter {

    lateinit var delegate: PinModule.IViewDelegate

    val setTitleLiveData = MutableLiveData<Int>()
    val setDescriptionLiveData = MutableLiveData<Int>()
    val highlightPinMaskLiveData = MutableLiveData<Int>()
    val showErrorLiveData = MutableLiveData<Int>()
    val showSuccessLiveData = MutableLiveData<Int>()

    val goToPinConfirmationLiveEvent = SingleLiveEvent<String>()
    val hideToolbarLiveEvent = SingleLiveEvent<Unit>()
    val unlockWalletLiveEvent = SingleLiveEvent<Unit>()
    val clearPinMaskWithDelayLiveEvent = SingleLiveEvent<Unit>()
    val showFingerprintDialogLiveEvent = SingleLiveEvent<Unit>()
    val minimizeAppLiveEvent = SingleLiveEvent<Unit>()
    val goBackLiveEvent = SingleLiveEvent<Unit>()

    fun init(interactionType: PinInteractionType, enteredPin: String) {
        PinModule.init(this, this, interactionType, enteredPin)
        delegate.viewDidLoad()
    }

    override fun setTitleForEnterPin() {
        setTitleLiveData.value = R.string.set_pin_title
    }

    override fun setDescriptionForEnterPin() {
        setDescriptionLiveData.value = R.string.set_pin_description
    }

    override fun highlightPinMask(length: Int) {
        highlightPinMaskLiveData.value = length
    }

    override fun showErrorShortPinLength() {
        showErrorLiveData.value = R.string.set_pin_error_short_pin
    }

    override fun setTitleForEnterAgain() {
        setTitleLiveData.value = R.string.set_pin_confirm_title
    }

    override fun setDescriptionForEnterAgain() {
        setDescriptionLiveData.value = R.string.set_pin_confirm_description
    }

    override fun showSuccessPinSet() {
        showSuccessLiveData.value = R.string.set_pin_success
    }

    override fun showErrorPinsDontMatch() {
        showErrorLiveData.value = R.string.set_pin_error_pins_dont_match
    }

    override fun showErrorFailedToSavePin() {
        showErrorLiveData.value = R.string.set_pin_error_failed_to_save_pin
    }

    override fun setDescriptionForUnlock() {
        setDescriptionLiveData.value = R.string.unlock_page_enter_your_pin
    }

    override fun hideToolbar() {
        hideToolbarLiveEvent.call()
    }

    override fun showErrorWrongPin() {
        showErrorLiveData.value = R.string.hud_text_invalid_pin_error
    }

    override fun clearPinMaskWithDelay() {
        clearPinMaskWithDelayLiveEvent.call()
    }

    override fun showFingerprintDialog() {
        showFingerprintDialogLiveEvent.call()
    }

    override fun minimizeApp() {
        minimizeAppLiveEvent.call()
    }

    override fun navigateToPrevPage() {
       goBackLiveEvent.call()
    }

    //IRouter
    override fun goToPinConfirmation(pin: String) {
        goToPinConfirmationLiveEvent.value = pin
    }

    override fun unlockWallet() {
        unlockWalletLiveEvent.call()
    }

}
