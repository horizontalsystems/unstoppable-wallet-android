package bitcoin.wallet.modules.pin

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.R
import bitcoin.wallet.SingleLiveEvent
import bitcoin.wallet.core.IKeyStoreSafeExecute

class PinViewModel : ViewModel(), PinModule.IView, PinModule.IRouter, IKeyStoreSafeExecute {

    lateinit var delegate: PinModule.IViewDelegate

    val title = MutableLiveData<Int>()
    val description = MutableLiveData<Int>()
    val highlightPinMask = MutableLiveData<Int>()
    val showErrorInDialog = MutableLiveData<Int>()
    val showErrorMessage = MutableLiveData<Int>()
    val showSuccess = SingleLiveEvent<Unit>()

    val goToSetPin = SingleLiveEvent<Unit>()
    val goToPinConfirmation = SingleLiveEvent<String>()
    val hideToolbar = SingleLiveEvent<Unit>()
    val unlockWallet = SingleLiveEvent<Unit>()
    val clearPinMaskWithDelay = SingleLiveEvent<Unit>()
    val hideBackButton = SingleLiveEvent<Unit>()
    val showFingerprintDialog = SingleLiveEvent<Unit>()
    val minimizeApp = SingleLiveEvent<Unit>()
    val goBack = SingleLiveEvent<Unit>()
    val goToPinEdit = SingleLiveEvent<Unit>()
    val keyStoreSafeExecute = SingleLiveEvent<Triple<Runnable, Runnable?, Runnable?>>()

    fun init(interactionType: PinInteractionType, enteredPin: String) {
        PinModule.init(this, this, this, interactionType, enteredPin)
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
        showErrorInDialog.value = R.string.set_pin_error_short_pin
    }

    override fun setTitleForEnterAgain() {
        title.value = R.string.set_pin_confirm_title
    }

    override fun setDescriptionForEnterAgain() {
        description.value = R.string.set_pin_confirm_description
    }

    override fun showSuccessPinSet() {
        showSuccess.call()
    }

    override fun showErrorPinsDontMatch() {
        showErrorMessage.value = R.string.set_pin_error_pins_dont_match
    }

    override fun showErrorFailedToSavePin() {
        showErrorInDialog.value = R.string.set_pin_error_failed_to_save_pin
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

    override fun hideBackButton() {
        hideBackButton.call()
    }

    override fun hideToolbar() {
        hideToolbar.call()
    }

    override fun showErrorWrongPin() {
        showErrorMessage.value = R.string.hud_text_invalid_pin_error
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

    override fun safeExecute(action: Runnable, onSuccess: Runnable?, onFailure: Runnable?) {
        keyStoreSafeExecute.value = Triple(action, onSuccess, onFailure)
    }

    //IRouter
    override fun goToSetPin() {
        goToSetPin.call()
    }

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
