package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.CoinException

class SendView : SendModule.IView {

    override lateinit var delegate: SendModule.IViewDelegate

    val error = MutableLiveData<Throwable>()
    val errorInDialog = SingleLiveEvent<CoinException>()
    val confirmationViewItems = MutableLiveData<List<SendModule.SendConfirmationViewItem>>()
    val showSendConfirmation = SingleLiveEvent<Unit>()
    val sendButtonEnabled = MutableLiveData<Boolean>()
    val inputItems = SingleLiveEvent<List<SendModule.Input>>()

    override fun loadInputItems(inputs: List<SendModule.Input>) {
        inputItems.value = inputs
    }

    override fun setSendButtonEnabled(enabled: Boolean) {
        sendButtonEnabled.postValue(enabled)
    }

    override fun showErrorInToast(error: Throwable) {
        this.error.value = error
    }

    override fun showErrorInDialog(coinException: CoinException) {
        errorInDialog.value = coinException
    }

    override fun showConfirmation(confirmationViewItems: List<SendModule.SendConfirmationViewItem>) {
        this.confirmationViewItems.value = confirmationViewItems
        showSendConfirmation.call()
    }

}
