package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.SendStateError
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import java.math.BigDecimal

class SendViewModel : ViewModel(), SendModule.IView {

    lateinit var delegate: SendModule.IViewDelegate

    val hintInfoLiveData = MutableLiveData<SendModule.HintInfo>()
    val sendButtonEnabledLiveData = MutableLiveData<Boolean>()
    val coinLiveData = MutableLiveData<Coin>()
    val amountInfoLiveData = MutableLiveData<SendModule.AmountInfo>()
    val switchButtonEnabledLiveData = MutableLiveData<Boolean>()
    val addressInfoLiveData = MutableLiveData<SendModule.AddressInfo>()
    val dismissLiveEvent = SingleLiveEvent<Unit>()
    val dismissWithSuccessLiveEvent = SingleLiveEvent<Unit>()
    val dismissConfirmationLiveEvent = SingleLiveEvent<Unit>()
    val feeInfoLiveData = MutableLiveData<SendModule.FeeInfo>()
    val errorLiveData = MutableLiveData<Int>()
    val sendConfirmationViewItemLiveData = MutableLiveData<SendModule.SendConfirmationViewItem>()
    val showConfirmationLiveEvent = SingleLiveEvent<Unit>()
    val pasteButtonEnabledLiveData = MutableLiveData<Boolean>()
    val feeIsAdjustableLiveData = MutableLiveData<Boolean>()
    val availableBalanceRetrievedLiveData = MutableLiveData<BigDecimal>()
    val onAddressParsedLiveData = MutableLiveData<PaymentRequestAddress>()
    val getParamsFromModulesLiveEvent = SingleLiveEvent<SendModule.ParamsAction>()
    val validationErrorLiveEvent = SingleLiveEvent<SendStateError.InsufficientAmount>()
    val insufficientFeeBalanceErrorLiveEvent = SingleLiveEvent<Pair<String, BigDecimal>>()
    val amountValidationLiveEvent = SingleLiveEvent<Unit>()
    val feeUpdatedLiveData = MutableLiveData<BigDecimal>()
    val mainInputTypeUpdatedLiveData = MutableLiveData<SendModule.InputType>()
    var decimalSize: Int? = null


    private var moduleInited = false

    fun init(coinCode: String) {
        hintInfoLiveData.value = null
        sendButtonEnabledLiveData.value = null
        coinLiveData.value = null
        amountInfoLiveData.value = null
        switchButtonEnabledLiveData.value = null
        addressInfoLiveData.value = null
        feeInfoLiveData.value = null
        errorLiveData.value = null
        sendConfirmationViewItemLiveData.value = null

        SendModule.init(this, coinCode)
        delegate.onViewDidLoad()
        moduleInited = true

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

    override fun dismiss() {
        dismissLiveEvent.call()
    }

    override fun onCleared() {
        if (moduleInited) {
            delegate.onClear()
        }
    }

    fun onViewResumed() {
        if (moduleInited) {
//            delegate.onViewResumed()
        }
    }

    override fun onAvailableBalanceRetrieved(availableBalance: BigDecimal) {
        availableBalanceRetrievedLiveData.value = availableBalance
    }

    override fun onAddressParsed(parsedAddress: PaymentRequestAddress) {
        onAddressParsedLiveData.value = parsedAddress
    }

    override fun getParamsForAction(paramsAction: SendModule.ParamsAction) {
        getParamsFromModulesLiveEvent.value = paramsAction
    }

    override fun onValidationError(error: SendStateError.InsufficientAmount) {
        validationErrorLiveEvent.value = error
    }

    override fun onInsufficientFeeBalance(coinCode: String, fee: BigDecimal) {
        insufficientFeeBalanceErrorLiveEvent.value = Pair(coinCode, fee)
    }

    override fun onAmountValidationSuccess() {
        amountValidationLiveEvent.call()
    }

    override fun onFeeUpdated(fee: BigDecimal) {
        feeUpdatedLiveData.value = fee
    }

    override fun onInputTypeUpdated(inputType: SendModule.InputType?) {
        mainInputTypeUpdatedLiveData.value = inputType
    }
}
