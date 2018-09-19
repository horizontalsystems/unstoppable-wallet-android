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

    fun init(interactionType: PinInteractionType, enteredPin: String) {
        PinModule.init(this, this, interactionType, enteredPin)
        delegate.viewDidLoad()
    }

    override fun setTitleEnterPin() {
        setTitleLiveData.value = R.string.set_pin_title
    }

    override fun setDescriptionEnterPin() {
        setDescriptionLiveData.value = R.string.set_pin_description
    }

    override fun highlightPinMask(length: Int) {
        highlightPinMaskLiveData.value = length
    }

    override fun showErrorShortPinLength() {
        showErrorLiveData.value = R.string.set_pin_error_short_pin
    }

    override fun setTitleEnterAgain() {
        setTitleLiveData.value = R.string.set_pin_confirm_title
    }

    override fun setDescriptionEnterAgain() {
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

    //IRouter
    override fun goToPinConfirmation(pin: String) {
        goToPinConfirmationLiveEvent.value = pin
    }

}
