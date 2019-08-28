package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.CoinException
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule

class SendViewModel : ViewModel(), SendModule.IView, SendModule.IRouter {

    override lateinit var delegate: SendModule.IViewDelegate

    var amountModuleDelegate: SendAmountModule.IAmountModuleDelegate? = null
    var addressModuleDelegate: SendAddressModule.IAddressModuleDelegate? = null
    var feeModuleDelegate: SendFeeModule.IFeeModuleDelegate? = null

    val closeWithSuccess = SingleLiveEvent<Unit>()
    val error = MutableLiveData<Throwable>()
    val errorInDialog = SingleLiveEvent<CoinException>()
    val confirmationViewItems = MutableLiveData<List<SendModule.SendConfirmationViewItem>>()
    val showSendConfirmation = SingleLiveEvent<Unit>()
    val sendButtonEnabled = MutableLiveData<Boolean>()
    val inputItems = SingleLiveEvent<List<SendModule.Input>>()

    val scanQrCode = SingleLiveEvent<Unit>()

    fun init(wallet: Wallet): SendModule.IViewDelegate {
        return SendModule.init(this, wallet)
    }

    override fun loadInputItems(inputs: List<SendModule.Input>) {
        inputItems.value = inputs
    }

    override fun setSendButtonEnabled(enabled: Boolean) {
        sendButtonEnabled.value = enabled
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
