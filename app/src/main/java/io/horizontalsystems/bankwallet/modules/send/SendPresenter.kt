package io.horizontalsystems.bankwallet.modules.send

import android.util.Log
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.SendStateError
import java.math.BigDecimal
import java.net.UnknownHostException

class SendPresenter(private val interactor: SendModule.IInteractor)
    : SendModule.IViewDelegate, SendModule.IInteractorDelegate, SendModule.ISendAmountPresenterDelegate {

    var view: SendViewModel? = null

//    private var feeItem = SendFeeInput(true)
//    private var sendButton = SendButton()

//    override val feeAdjustable: Boolean
//        get() = true

    //
    // IViewDelegate
    //
    override fun onViewDidLoad() {
        view?.setCoin(interactor.coin)
    }

    override fun onGetAvailableBalance() {
        view?.getParamsForAction(SendModule.ParamsAction.AvailableBalance)
    }

    override fun onAmountChanged(coinAmount: BigDecimal?) {
        if (coinAmount == null) {
            return
        }

        view?.getParamsForAction(SendModule.ParamsAction.Validate)
    }

    override fun onParamsFetchedForAction(params: MutableMap<SendModule.AdapterFields, Any?>, paramsAction: SendModule.ParamsAction) {
        when (paramsAction) {
            SendModule.ParamsAction.Validate -> {
                val coinAmount = params[SendModule.AdapterFields.Amount] as? BigDecimal ?: BigDecimal.ZERO
                if (coinAmount > BigDecimal.ZERO) {
                    interactor.validate(params)
                } else {
                    // amount is empty. Reset error and Just set 0 for all
                }
            }
            SendModule.ParamsAction.AvailableBalance -> getAvailableBalance(params)
        }
    }

    private fun getAvailableBalance(params: MutableMap<SendModule.AdapterFields, Any?>) {
        val address = params[SendModule.AdapterFields.Address] as? String
        val feeRatePriority = params[SendModule.AdapterFields.FeeRatePriority] as? FeeRatePriority
                ?: FeeRatePriority.MEDIUM
        val availableBalance = interactor.getAvailableBalance(address, feeRatePriority)

        view?.onAvailableBalanceRetrieved(availableBalance)
    }

    override fun onAddressChanged() {
        view?.getParamsForAction(SendModule.ParamsAction.Validate)
    }

    override fun parseAddress(address: String) {
        val parsedAddress = interactor.parsePaymentAddress(address)
        view?.onAddressParsed(parsedAddress)
    }

    override fun onValidationComplete(errorList: List<SendStateError>) {
        Log.e("SendPresenter", "onValidationComplete")
        errorList.forEach { error ->
            when(error) {
                is SendStateError.InsufficientAmount -> {
                    val balance = error.balance

                }
                is SendStateError.InsufficientFeeBalance -> {

                }
            }
        }

    }

    override fun onSendClicked() {
//        val state = interactor.stateForUserInput(userInput)
//        val viewItem = factory.confirmationViewItemForState(state) ?: return
//
//        view?.showConfirmation(viewItem)
    }

    override fun onConfirmClicked() {
//        interactor.send(userInput)
    }

    override fun onClear() {
        interactor.clear()
    }

    override fun didSend() {
        view?.dismissWithSuccess()
    }

    override fun didFailToSend(error: Throwable) {
        val textResourceId = getErrorText(error)
        view?.showError(textResourceId)
    }

    private fun getErrorText(error: Throwable): Int {
        return when (error) {
            is UnknownHostException -> R.string.Hud_Text_NoInternet
            else -> R.string.Hud_Network_Issue
        }
    }



    //    override fun onViewResumed() {
//        interactor.retrieveRate()
//    }

//    override fun onMaxClicked() {
//        val totalBalanceMinusFee = interactor.getTotalBalanceMinusFee(userInput.inputType, userInput.address, userInput.feePriority)
//        userInput.amount = totalBalanceMinusFee
//
//        val state = interactor.stateForUserInput(userInput)
//        val viewItem = factory.viewItemForState(state, true)
//
//        view?.setAmountInfo(viewItem.amountInfo)
//    }
//
//    override fun onSwitchClicked() {
//        val convertedAmount = interactor.convertedAmountForInputType(userInput.inputType, userInput.amount)
//                ?: return
//
//        val newInputType = when (userInput.inputType) {
//            SendModule.InputType.CURRENCY -> SendModule.InputType.COIN
//            else -> SendModule.InputType.CURRENCY
//        }
//
//        userInput.amount = convertedAmount
//        userInput.inputType = newInputType
//
//        val state = interactor.stateForUserInput(userInput)
//        val viewItem = factory.viewItemForState(state)
//
//        view?.setDecimal(viewItem.decimal)
//        view?.setAmountInfo(viewItem.amountInfo)
//        view?.setHintInfo(viewItem.hintInfo)
//        view?.setFeeInfo(viewItem.feeInfo)
//
////        updateInputElements(viewItem)
//
//        interactor.defaultInputType = newInputType
//    }

//    override fun onPasteClicked() {
//        interactor.addressFromClipboard?.let {
//            onAddressEnter(it)
//        }
//    }
//
//    override fun onScanAddress(address: String) {
//        onAddressEnter(address)
//    }
//
//    override fun onDeleteClicked() {
//        onAddressChange(null)
//        updatePasteButtonState()
//    }

//    override fun onFeeSliderChange(value: Int) {
//        userInput.feePriority = FeeRatePriority.valueOf(value)
//
//        val state = interactor.stateForUserInput(userInput)
//        val viewItem = factory.viewItemForState(state)
//
//        view?.setHintInfo(viewItem.hintInfo)
//        view?.setFeeInfo(viewItem.feeInfo)
//        view?.setSendButtonEnabled(viewItem.sendButtonEnabled)
//    }

    //
    // IInteractorDelegate
    //
//    override fun didFeeRateRetrieve() {
//        updateViewItem()
//    }
//
//    override fun didRateRetrieve(rate: Rate?) {
//        if (rate == null) {
//            if (userInput.inputType == SendModule.InputType.CURRENCY) {
//                userInput.amount = BigDecimal.ZERO
//            }
//            userInput.inputType = SendModule.InputType.COIN
//        } else if (interactor.defaultInputType == SendModule.InputType.CURRENCY && userInput.amount == BigDecimal.ZERO) {
//            userInput.inputType = interactor.defaultInputType
//        }
//
//        updateViewItem()
//    }

//    private fun updateViewItem() {
//        val state = interactor.stateForUserInput(userInput)
//        val viewItem = factory.viewItemForState(state)
//
//        view?.setDecimal(viewItem.decimal)
//        view?.setAmountInfo(viewItem.amountInfo)
//        view?.setSwitchButtonEnabled(viewItem.switchButtonEnabled)
//        view?.setHintInfo(viewItem.hintInfo)
//        view?.setFeeInfo(viewItem.feeInfo)
//    }

    //
    // Private
    //
//    private fun updatePasteButtonState() {
//        view?.setPasteButtonState(interactor.clipboardHasPrimaryClip)
//    }
//
//    private fun onAddressEnter(address: String) {
//        val paymentAddress = interactor.parsePaymentAddress(address)
//        paymentAddress.amount?.let {
//            userInput.amount = it
//        }
//
//        onAddressChange(paymentAddress.address)
//    }

}
