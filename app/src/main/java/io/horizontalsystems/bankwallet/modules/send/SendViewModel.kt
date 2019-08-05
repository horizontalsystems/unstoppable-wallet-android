package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.SendStateError
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.SendConfirmationInfo
import java.math.BigDecimal

class SendViewModel : ViewModel(), SendModule.IView {

    lateinit var delegate: SendModule.IViewDelegate

    val dismissWithSuccessLiveEvent = SingleLiveEvent<Unit>()
    val errorLiveData = MutableLiveData<Int?>()
    val sendConfirmationLiveData = MutableLiveData<SendConfirmationInfo>()
    val showSendConfirmationLiveData = SingleLiveEvent<Unit>()
    val availableBalanceRetrievedLiveData = MutableLiveData<BigDecimal>()
    val onAddressParsedLiveData = MutableLiveData<PaymentRequestAddress>()
    val getParamsFromModulesLiveEvent = SingleLiveEvent<SendModule.ParamsAction>()
    val validationErrorLiveEvent = SingleLiveEvent<SendStateError.InsufficientAmount>()
    val insufficientFeeBalanceErrorLiveEvent = SingleLiveEvent<Pair<String, BigDecimal>>()
    val amountValidationLiveEvent = SingleLiveEvent<Unit>()
    val feeUpdatedLiveData = MutableLiveData<BigDecimal>()
    val mainInputTypeUpdatedLiveData = MutableLiveData<SendModule.InputType>()
    val sendButtonEnabledLiveData = MutableLiveData<Boolean>()
    val fetchStatesFromModulesLiveEvent = SingleLiveEvent<Unit>()
    val inputItemsLiveEvent = SingleLiveEvent<List<SendModule.Input>>()


    fun init(coinCode: String) {
        SendModule.init(this, coinCode)
        delegate.onViewDidLoad()
    }

    override fun loadInputItems(inputs: List<SendModule.Input>) {
        inputItemsLiveEvent.value = inputs
    }

    override fun setSendButtonEnabled(sendButtonEnabled: Boolean) {
        sendButtonEnabledLiveData.value = sendButtonEnabled
    }

    override fun showConfirmation(viewItem: SendConfirmationInfo) {
        sendConfirmationLiveData.value = viewItem
        showSendConfirmationLiveData.call()
    }

    override fun showError(error: Int) {
        errorLiveData.value = error
    }

    override fun dismissWithSuccess() {
        dismissWithSuccessLiveEvent.call()
    }

    override fun onCleared() {
        delegate.onClear()
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

    override fun getValidStatesFromModules() {
        fetchStatesFromModulesLiveEvent.call()
    }

}
