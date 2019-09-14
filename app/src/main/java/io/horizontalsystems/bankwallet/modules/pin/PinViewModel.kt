package io.horizontalsystems.bankwallet.modules.pin

import androidx.biometric.BiometricPrompt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.pin.edit.EditPinModule
import io.horizontalsystems.bankwallet.modules.pin.set.SetPinModule
import io.horizontalsystems.bankwallet.modules.pin.unlock.UnlockPinModule
import java.util.*

class PinViewModel : ViewModel(), PinModule.IPinView, SetPinModule.ISetPinRouter, EditPinModule.IEditPinRouter, UnlockPinModule.IUnlockPinRouter {

    lateinit var delegate: PinModule.IPinViewDelegate
    val titleLiveDate = MutableLiveData<Int>()
    val addPagesEvent = MutableLiveData<List<PinPage>>()
    val showPageAtIndex = MutableLiveData<Int>()
    val showError = MutableLiveData<Int>()
    val updateTopTextForPage = MutableLiveData<Pair<TopText, Int>>()
    val fillPinCircles = MutableLiveData<Pair<Int, Int>>()
    val navigateToMainLiveEvent = SingleLiveEvent<Unit>()
    val hideToolbar = SingleLiveEvent<Unit>()
    val dismissWithSuccessLiveEvent = SingleLiveEvent<Unit>()
    val showBackButton = SingleLiveEvent<Unit>()
    val showFingerprintInputLiveEvent = SingleLiveEvent<BiometricPrompt.CryptoObject>()
    val resetCirclesWithShakeAndDelayForPage = SingleLiveEvent<Int>()
    val showLockedView = SingleLiveEvent<Date>()
    val showPinInput = SingleLiveEvent<Unit>()


    fun init(interactionType: PinInteractionType, showCancelButton: Boolean) {
        when (interactionType) {
            PinInteractionType.SET_PIN -> SetPinModule.init(this, this)
            PinInteractionType.UNLOCK -> UnlockPinModule.init(this, this, showCancelButton)
            PinInteractionType.EDIT_PIN -> EditPinModule.init(this, this)
        }
        delegate.viewDidLoad()
    }

    override fun setTitle(title: Int) {
        titleLiveDate.value = title
    }

    override fun hideToolbar() {
        hideToolbar.call()
    }

    override fun addPages(pages: List<PinPage>) {
        addPagesEvent.value = pages
    }

    override fun showPage(index: Int) {
        showPageAtIndex.value = index
    }

    override fun updateTopTextForPage(text: TopText, pageIndex: Int) {
        updateTopTextForPage.value = Pair(text, pageIndex)
    }

    override fun showError(error: Int) {
        showError.value = error
    }

    override fun showPinWrong(pageIndex: Int) {
        resetCirclesWithShakeAndDelayForPage.value = pageIndex
    }

    override fun showFingerprintDialog(cryptoObject: BiometricPrompt.CryptoObject) {
        showFingerprintInputLiveEvent.postValue(cryptoObject)
    }

    override fun showBackButton() {
        showBackButton.call()
    }

    override fun fillCircles(length: Int, pageIndex: Int) {
        fillPinCircles.value = Pair(length, pageIndex)
    }

    override fun navigateToMain() {
        navigateToMainLiveEvent.call()
    }

    override fun dismissModuleWithSuccess() {
        dismissWithSuccessLiveEvent.call()
    }

    override fun showLockView(until: Date) {
        showLockedView.postValue(until)
    }

    override fun showPinInput() {
        showPinInput.call()
    }
}
