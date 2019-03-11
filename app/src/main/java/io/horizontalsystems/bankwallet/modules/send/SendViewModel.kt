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
    val dismissConfirmationLiveEvent = SingleLiveEvent<Unit>()
    val feeInfoLiveData = MutableLiveData<SendModule.FeeInfo>()
    val errorLiveData = MutableLiveData<Int>()
    val sendConfirmationViewItemLiveData = MutableLiveData<SendModule.SendConfirmationViewItem>()
    val showConfirmationLiveEvent = SingleLiveEvent<Unit>()
    val feeSliderProgressLiveEvent = SingleLiveEvent<Int>()
    val pasteButtonEnabledLiveData = MutableLiveData<Boolean>()
    val feeIsAdjustableLiveData = MutableLiveData<Boolean>()
    var decimalSize: Int? = null

    fun init(coin: String) {
        hintInfoLiveData.value = null
        sendButtonEnabledLiveData.value = null
        coinLiveData.value = null
        amountInfoLiveData.value = null
        switchButtonEnabledLiveData.value = null
        addressInfoLiveData.value = null
        feeInfoLiveData.value = null
        errorLiveData.value = null
        sendConfirmationViewItemLiveData.value = null

        SendModule.init(this, coin)
        delegate.onViewDidLoad()
        feeIsAdjustableLiveData.value = delegate.feeAdjustable
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

    override fun setFeeInfo(feeInfo: SendModule.FeeInfo?) {
        feeInfoLiveData.value = feeInfo
    }

    override fun setSendButtonEnabled(sendButtonEnabled: Boolean) {
        sendButtonEnabledLiveData.value = sendButtonEnabled
    }

    override fun showConfirmation(viewItem: SendModule.SendConfirmationViewItem) {
        sendConfirmationViewItemLiveData.value = viewItem
        showConfirmationLiveEvent.call()
    }

    override fun showError(error: Int) {
        errorLiveData.value = error
    }

    override fun dismissWithSuccess() {
        dismissWithSuccessLiveEvent.call()
        dismissConfirmationLiveEvent.call()
    }

    override fun setFeeSliderPosition(sliderProgress: Int?) {
        feeSliderProgressLiveEvent.value = sliderProgress
    }

    override fun onCleared() {
        delegate.onClear()
    }

}
