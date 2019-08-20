package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.SendConfirmationInfo
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule

class SendViewModel : ViewModel(), SendModule.IView, SendModule.IRouter {

    lateinit var delegate: SendModule.IViewDelegate

    var amountModuleDelegate: SendAmountModule.IAmountModuleDelegate? = null
    var addressModuleDelegate: SendAddressModule.IAddressModuleDelegate? = null
    var feeModuleDelegate: SendFeeModule.IFeeModuleDelegate? = null

    val closeWithSuccess = SingleLiveEvent<Unit>()
    val errorLiveData = MutableLiveData<Throwable>()
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

    override fun showError(error: Throwable) {
        errorLiveData.value = error
    }

    override fun showConfirmation(viewItem: SendConfirmationInfo) {
        sendConfirmationLiveData.value = viewItem
        showSendConfirmationLiveData.call()
    }

    // SendModule.IRouter

    override fun scanQrCode() {
        scanQrCode.call()
    }

    override fun closeWithSuccess() {
        closeWithSuccess.call()
    }

    override fun onCleared() {
        delegate.onClear()
    }

}
