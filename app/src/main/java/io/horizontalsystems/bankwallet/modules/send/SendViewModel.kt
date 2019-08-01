package io.horizontalsystems.bankwallet.modules.send

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.SendStateError
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import java.math.BigDecimal

class SendViewModel : ViewModel(), SendModule.IView {

    lateinit var delegate: SendModule.IViewDelegate

    val dismissConfirmationLiveEvent = SingleLiveEvent<Unit>()
    val dismissWithSuccessLiveEvent = SingleLiveEvent<Unit>()
    val errorLiveData = MutableLiveData<Int>()
    val sendConfirmationViewItemLiveData = MutableLiveData<SendModule.SendConfirmationViewItem>()
    val showConfirmationLiveEvent = SingleLiveEvent<Unit>()
    val availableBalanceRetrievedLiveData = MutableLiveData<BigDecimal>()
    val onAddressParsedLiveData = MutableLiveData<PaymentRequestAddress>()
    val getParamsFromModulesLiveEvent = SingleLiveEvent<SendModule.ParamsAction>()
    val validationErrorLiveEvent = SingleLiveEvent<SendStateError.InsufficientAmount>()
    val insufficientFeeBalanceErrorLiveEvent = SingleLiveEvent<Pair<String, BigDecimal>>()
    val amountValidationLiveEvent = SingleLiveEvent<Unit>()
    val feeUpdatedLiveData = MutableLiveData<BigDecimal>()
    val mainInputTypeUpdatedLiveData = MutableLiveData<SendModule.InputType>()
    val sendButtonEnabledLiveData = MutableLiveData<Boolean>()


    private var moduleInited = false

    fun init(coinCode: String) {
        errorLiveData.value = null
        sendConfirmationViewItemLiveData.value = null

        SendModule.init(this, coinCode)
        moduleInited = true
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

    override fun onCleared() {
        if (moduleInited) {
            delegate.onClear()
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
