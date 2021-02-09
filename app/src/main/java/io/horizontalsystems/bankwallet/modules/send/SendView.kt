package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent

class SendView : SendModule.IView {

    override lateinit var delegate: SendModule.IViewDelegate

    val error = SingleLiveEvent<Throwable>()
    val confirmationViewItems = MutableLiveData<List<SendModule.SendConfirmationViewItem>>()
    val showSendConfirmation = SingleLiveEvent<Unit>()
    val sendButtonEnabled = MutableLiveData<SendPresenter.ActionState>()
    val inputItems = SingleLiveEvent<List<SendModule.Input>>()

    override fun loadInputItems(inputs: List<SendModule.Input>) {
        inputItems.value = inputs
    }

    override fun setSendButtonEnabled(actionState: SendPresenter.ActionState) {
        sendButtonEnabled.postValue(actionState)
    }

    override fun showErrorInToast(error: Throwable) {
        this.error.value = error
    }

    override fun showConfirmation(confirmationViewItems: List<SendModule.SendConfirmationViewItem>) {
        this.confirmationViewItems.value = confirmationViewItems
        showSendConfirmation.call()
    }

}
