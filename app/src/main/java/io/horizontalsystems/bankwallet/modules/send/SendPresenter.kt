package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.SendStateError
import io.horizontalsystems.bankwallet.core.WrongParameters
import java.math.BigDecimal
import java.net.UnknownHostException

class SendPresenter(private val interactor: SendModule.IInteractor)
    : SendModule.IViewDelegate, SendModule.IInteractorDelegate, SendModule.ISendAmountPresenterDelegate {

    var view: SendViewModel? = null

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

        updateModules()
    }

    override fun onParamsFetchedForAction(params: Map<SendModule.AdapterFields, Any?>, paramsAction: SendModule.ParamsAction) {
        when (paramsAction) {
            SendModule.ParamsAction.UpdateModules -> {
                val updatedParams = params.toMutableMap()
                val amount = (params[SendModule.AdapterFields.Amount] as? BigDecimal)
                if (amount == null) {
                    updatedParams[SendModule.AdapterFields.Amount] = BigDecimal.ZERO
                }

                interactor.validate(updatedParams)
                interactor.updateFee(updatedParams)
            }
            SendModule.ParamsAction.AvailableBalance -> getAvailableBalance(params)
        }
    }

    override fun onAddressChanged() {
        updateModules()
    }

    override fun parseAddress(address: String) {
        val parsedAddress = interactor.parsePaymentAddress(address)
        view?.onAddressParsed(parsedAddress)
    }

    override fun onValidationComplete(errorList: List<SendStateError>) {
        var amountValidationSuccess = true
        errorList.forEach { error ->
            when (error) {
                is SendStateError.InsufficientAmount -> {
                    amountValidationSuccess = false
                    view?.onValidationError(error)
                }
                is SendStateError.InsufficientFeeBalance -> {
                    view?.onInsufficientFeeBalance(interactor.coin.code, error.fee)
                }
            }
        }
        if (amountValidationSuccess) {
            view?.onAmountValidationSuccess()
        }
    }

    override fun onFeeUpdated(fee: BigDecimal) {
        view?.onFeeUpdated(fee)
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

    override fun onFeePriorityChange(feeRatePriority: FeeRatePriority) {
        updateModules()
    }

    private fun updateModules() {
        view?.getParamsForAction(SendModule.ParamsAction.UpdateModules)
    }

    override fun onInputTypeUpdated(inputType: SendModule.InputType?) {
        view?.onInputTypeUpdated(inputType)
    }

    private fun getAvailableBalance(params: Map<SendModule.AdapterFields, Any?>) {
        var availableBalance = BigDecimal.ZERO
        try {
            availableBalance = interactor.getAvailableBalance(params)
        } catch (e: WrongParameters) {
            //wrong parameters exception
        }

        view?.onAvailableBalanceRetrieved(availableBalance)
    }

    private fun getErrorText(error: Throwable): Int {
        return when (error) {
            is UnknownHostException -> R.string.Hud_Text_NoInternet
            else -> R.string.Hud_Network_Issue
        }
    }

}
