package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.sendviews.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.sendviews.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.SendConfirmationInfo
import io.horizontalsystems.bankwallet.modules.send.sendviews.fee.SendFeeModule

class SendViewModel : ViewModel(), SendModule.IView, SendModule.IRouter {

    lateinit var delegate: SendModule.IViewDelegate

    var amountModuleDelegate: SendAmountModule.IAmountModuleDelegate? = null
    var addressModuleDelegate: SendAddressModule.IAddressModuleDelegate? = null
    var feeModuleDelegate: SendFeeModule.IFeeModuleDelegate? = null

    val dismissWithSuccessLiveEvent = SingleLiveEvent<Unit>()
    val errorLiveData = MutableLiveData<Int?>()
    val sendConfirmationLiveData = MutableLiveData<SendConfirmationInfo>()
    val showSendConfirmationLiveData = SingleLiveEvent<Unit>()
    val sendButtonEnabledLiveData = MutableLiveData<Boolean>()
    val inputItemsLiveEvent = SingleLiveEvent<List<SendModule.Input>>()

    val scanQrCode = SingleLiveEvent<Unit>()

    fun init(wallet: Wallet): SendModule.IViewDelegate {
        return SendModule.init(this, wallet)
    }

    override fun loadInputItems(inputs: List<SendModule.Input>) {
        inputItemsLiveEvent.value = inputs
    }

    override fun setSendButtonEnabled(enabled: Boolean) {
        sendButtonEnabledLiveData.value = enabled
    }

    override fun dismissWithSuccess() {
        dismissWithSuccessLiveEvent.call()
    }

    override fun showError(error: Int) {
        errorLiveData.value = error
    }

    // SendModule.IRouter

    override fun scanQrCode() {
        scanQrCode.call()
    }

    override fun showConfirmation(viewItem: SendConfirmationInfo) {
        sendConfirmationLiveData.value = viewItem
        showSendConfirmationLiveData.call()
    }

    override fun onCleared() {
        delegate.onClear()
    }

}
