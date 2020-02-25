package io.horizontalsystems.pin

import androidx.biometric.BiometricPrompt
import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent
import java.util.*

class PinView : PinModule.IView {

    val title = MutableLiveData<Int>()
    val addPages = MutableLiveData<List<PinPage>>()
    val showPageAtIndex = MutableLiveData<Int>()
    val showError = MutableLiveData<Int>()
    val updateTopTextForPage = MutableLiveData<Pair<TopText, Int>>()
    val fillPinCircles = MutableLiveData<Pair<Int, Int>>()
    val hideToolbar = SingleLiveEvent<Unit>()
    val showBackButton = SingleLiveEvent<Unit>()
    val showFingerprintButton = SingleLiveEvent<Unit>()
    val showFingerprintInput = SingleLiveEvent<BiometricPrompt.CryptoObject>()
    val resetCirclesWithShakeAndDelayForPage = SingleLiveEvent<Int>()
    val showLockedView = SingleLiveEvent<Date>()
    val showPinInput = SingleLiveEvent<Unit>()


    override fun setTitle(title: Int) {
        this.title.value = title
    }

    override fun hideToolbar() {
        hideToolbar.call()
    }

    override fun addPages(pages: List<PinPage>) {
        addPages.value = pages
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

    override fun showFingerprintButton() {
        showFingerprintButton.call()
    }

    override fun showFingerprintDialog(cryptoObject: BiometricPrompt.CryptoObject) {
        showFingerprintInput.postValue(cryptoObject)
    }

    override fun showBackButton() {
        showBackButton.call()
    }

    override fun fillCircles(length: Int, pageIndex: Int) {
        fillPinCircles.value = Pair(length, pageIndex)
    }

    override fun showLockView(until: Date) {
        showLockedView.postValue(until)
    }

    override fun showPinInput() {
        showPinInput.call()
    }
}
