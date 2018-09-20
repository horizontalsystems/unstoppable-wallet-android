package bitcoin.wallet.modules.pin

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.R
import bitcoin.wallet.SingleLiveEvent

class PinViewModel : ViewModel(), PinModule.IView, PinModule.IRouter {

    lateinit var delegate: PinModule.IViewDelegate

    val title = MutableLiveData<Int>()
    val description = MutableLiveData<Int>()
    val highlightPinMask = MutableLiveData<Int>()
    val showError = MutableLiveData<Int>()
    val showSuccess = MutableLiveData<Int>()

    val goToPinConfirmation = SingleLiveEvent<String>()
    val hideToolbar = SingleLiveEvent<Unit>()
    val unlockWallet = SingleLiveEvent<Unit>()
    val clearPinMaskWithDelay = SingleLiveEvent<Unit>()
    val showFingerprintDialog = SingleLiveEvent<Unit>()
    val minimizeApp = SingleLiveEvent<Unit>()
    val goBack = SingleLiveEvent<Unit>()
    val goToPinEdit = SingleLiveEvent<Unit>()

    fun init(interactionType: PinInteractionType, enteredPin: String) {
        PinModule.init(this, this, interactionType, enteredPin)
        delegate.viewDidLoad()
    }

    override fun setTitleForEnterPin() {
        title.value = R.string.set_pin_title
    }

    override fun setDescriptionForEnterPin() {
        description.value = R.string.set_pin_description
    }

    override fun highlightPinMask(length: Int) {
        highlightPinMask.value = length
    }

    override fun showErrorShortPinLength() {
        showError.value = R.string.set_pin_error_short_pin
    }

    override fun setTitleForEnterAgain() {
        title.value = R.string.set_pin_confirm_title
    }

    override fun setDescriptionForEnterAgain() {
        description.value = R.string.set_pin_confirm_description
    }

    override fun showSuccessPinSet() {
        showSuccess.value = R.string.set_pin_success
    }

    override fun showErrorPinsDontMatch() {
        showError.value = R.string.set_pin_error_pins_dont_match
    }

    override fun showErrorFailedToSavePin() {
        showError.value = R.string.set_pin_error_failed_to_save_pin
    }

    override fun setDescriptionForUnlock() {
        description.value = R.string.unlock_page_enter_your_pin
    }

    override fun setTitleForEditPinAuth() {
        title.value = R.string.edit_pin_auth_title
    }

    override fun setDescriptionForEditPinAuth() {
        description.value = R.string.edit_pin_auth_description
    }

    override fun setTitleForEditPin() {
        title.value = R.string.edit_pin_title
    }

    override fun setDescriptionForEditPin() {
        description.value = R.string.edit_pin_description
    }

    override fun hideToolbar() {
        hideToolbar.call()
    }

    override fun showErrorWrongPin() {
        showError.value = R.string.hud_text_invalid_pin_error
    }

    override fun clearPinMaskWithDelay() {
        clearPinMaskWithDelay.call()
    }

    override fun showFingerprintDialog() {
        showFingerprintDialog.call()
    }

    override fun minimizeApp() {
        minimizeApp.call()
    }

    override fun navigateToPrevPage() {
        goBack.call()
    }

    //IRouter
    override fun goToPinConfirmation(pin: String) {
        goToPinConfirmation.value = pin
    }

    override fun unlockWallet() {
        unlockWallet.call()
    }

    override fun goToPinEdit() {
        goToPinEdit.call()
    }

}
