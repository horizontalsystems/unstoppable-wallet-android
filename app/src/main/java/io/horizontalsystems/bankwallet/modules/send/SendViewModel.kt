package io.horizontalsystems.bankwallet.modules.send

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin

class SendViewModel : ViewModel(), SendModule.IView {

    lateinit var delegate: SendModule.IViewDelegate

    val hintInfoLiveData = MutableLiveData<SendModule.HintInfo>()
    val sendButtonEnabledLiveData = MutableLiveData<Boolean>()
    val coinLiveData = MutableLiveData<Coin>()
    val amountInfoLiveData = MutableLiveData<SendModule.AmountInfo>()
    val switchButtonEnabledLiveData = MutableLiveData<Boolean>()
    val addressInfoLiveData = MutableLiveData<SendModule.AddressInfo>()
    val dismissWithSuccessLiveEvent = SingleLiveEvent<Unit>()
    val primaryFeeAmountInfoLiveData = MutableLiveData<SendModule.AmountInfo>()
    val secondaryFeeAmountInfoLiveData = MutableLiveData<SendModule.AmountInfo>()
    val errorLiveData = MutableLiveData<Throwable>()
    val sendConfirmationViewItemLiveData = MutableLiveData<SendModule.SendConfirmationViewItem>()
    val showConfirmationLiveEvent = SingleLiveEvent<Unit>()
    val pasteButtonEnabledLiveData = MutableLiveData<Boolean>()
    var decimalSize: Int? = null

    fun init(coin: String) {
        hintInfoLiveData.value = null
        sendButtonEnabledLiveData.value = null
        coinLiveData.value = null
        amountInfoLiveData.value = null
        switchButtonEnabledLiveData.value = null
        addressInfoLiveData.value = null
        primaryFeeAmountInfoLiveData.value = null
        secondaryFeeAmountInfoLiveData.value = null
        errorLiveData.value = null
        sendConfirmationViewItemLiveData.value = null

        SendModule.init(this, coin)
        delegate.onViewDidLoad()
    }

    override fun setPasteButtonState(enabled: Boolean) {
        pasteButtonEnabledLiveData.value = enabled
    }

    override fun setCoin(coin: Coin) {
        coinLiveData.value = coin
    }

    override fun setDecimal(decimal: Int) {
        decimalSize = decimal
    }

    override fun setAmountInfo(amountInfo: SendModule.AmountInfo?) {
        amountInfoLiveData.value = amountInfo
    }

    override fun setSwitchButtonEnabled(enabled: Boolean) {
        switchButtonEnabledLiveData.value = enabled
    }

    override fun setHintInfo(amountInfo: SendModule.HintInfo?) {
        hintInfoLiveData.value = amountInfo
    }

    override fun setAddressInfo(addressInfo: SendModule.AddressInfo?) {
        addressInfoLiveData.value = addressInfo
    }

    override fun setPrimaryFeeInfo(primaryFeeInfo: SendModule.AmountInfo?) {
        primaryFeeAmountInfoLiveData.value = primaryFeeInfo
    }

    override fun setSecondaryFeeInfo(secondaryFeeInfo: SendModule.AmountInfo?) {
        secondaryFeeAmountInfoLiveData.value = secondaryFeeInfo
    }

    override fun setSendButtonEnabled(sendButtonEnabled: Boolean) {
        sendButtonEnabledLiveData.value = sendButtonEnabled
    }

    override fun showConfirmation(viewItem: SendModule.SendConfirmationViewItem) {
        sendConfirmationViewItemLiveData.value = viewItem
        showConfirmationLiveEvent.call()
    }

    override fun showError(error: Throwable) {
        errorLiveData.value = error
    }

    override fun dismissWithSuccess() {
        dismissWithSuccessLiveEvent.call()
    }

}
