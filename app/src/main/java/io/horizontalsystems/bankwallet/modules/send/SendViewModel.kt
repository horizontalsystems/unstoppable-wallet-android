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

    val dismissConfirmationLiveEvent = SingleLiveEvent<Unit>()
    val dismissWithSuccessLiveEvent = SingleLiveEvent<Unit>()
    val errorLiveData = MutableLiveData<Int?>()
    val showSendConfirmationLiveData = SingleLiveEvent<SendConfirmationInfo>()
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


    fun init(coinCode: String) {
        SendModule.init(this, coinCode)
    }

    override fun setSendButtonEnabled(sendButtonEnabled: Boolean) {
        sendButtonEnabledLiveData.value = sendButtonEnabled
    }

    override fun showConfirmation(viewItem: SendConfirmationInfo) {
        showSendConfirmationLiveData.value = viewItem
    }

    override fun showError(error: Int) {
        errorLiveData.value = error
    }

    override fun dismissWithSuccess() {
        dismissWithSuccessLiveEvent.call()
        dismissConfirmationLiveEvent.call()
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
