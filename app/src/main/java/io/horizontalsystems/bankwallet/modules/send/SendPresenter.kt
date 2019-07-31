package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.SendStateError
import io.horizontalsystems.bankwallet.core.WrongParameters
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.math.BigDecimal
import java.net.UnknownHostException

class SendPresenter(
        private val interactor: SendModule.IInteractor,
        private val confirmationFactory: ConfirmationViewItemFactory)
    : SendModule.IViewDelegate, SendModule.IInteractorDelegate, SendModule.ISendAmountPresenterDelegate {

    var view: SendViewModel? = null

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
                val coinValue = (params[SendModule.AdapterFields.CoinValue] as? CoinValue)
                updatedParams[SendModule.AdapterFields.CoinAmount] = coinValue?.value ?: BigDecimal.ZERO

                interactor.validate(updatedParams)
                interactor.updateFee(updatedParams)
            }
            SendModule.ParamsAction.AvailableBalance -> getAvailableBalance(params)
            SendModule.ParamsAction.ShowConfirm -> showConfirmationDialog(params)
            SendModule.ParamsAction.Send -> send(params)
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
        view?.getParamsForAction(SendModule.ParamsAction.ShowConfirm)
    }

    override fun onConfirmClicked() {
        view?.getParamsForAction(SendModule.ParamsAction.Send)
    }

    override fun onClear() {
        interactor.clear()
    }

    override fun didSend() {
        view?.dismissWithSuccess()
    }

    override fun showError(error: Throwable) {
        val textResourceId = getErrorText(error)
        view?.showError(textResourceId)
    }

    override fun onFeePriorityChange(feeRatePriority: FeeRatePriority) {
        updateModules()
    }

    private fun showConfirmationDialog(params: Map<SendModule.AdapterFields, Any?>) {
        try {
            val inputType: SendModule.InputType = params[SendModule.AdapterFields.InputType] as SendModule.InputType
            val address: String = (params[SendModule.AdapterFields.Address] as? String) ?: throw WrongParameters()
            val coinValue: CoinValue = (params[SendModule.AdapterFields.CoinValue] as? CoinValue) ?: throw WrongParameters()
            val currencyValue: CurrencyValue? = params[SendModule.AdapterFields.CurrencyValue] as? CurrencyValue
            val feeCoinValue: CoinValue = (params[SendModule.AdapterFields.FeeCoinValue] as? CoinValue) ?: throw WrongParameters()
            val feeCurrencyValue: CurrencyValue? = params[SendModule.AdapterFields.FeeCurrencyValue] as? CurrencyValue

            val confirmationViewItem = confirmationFactory.confirmationViewItem(
                    interactor.coin,
                    inputType,
                    address,
                    coinValue,
                    currencyValue,
                    feeCoinValue,
                    feeCurrencyValue
            )

            view?.showConfirmation(confirmationViewItem)
        } catch (error: WrongParameters) {
            //wrong parameters exception
        }
    }

    private fun send(params: Map<SendModule.AdapterFields, Any?>) {
        try{
            val address: String = (params[SendModule.AdapterFields.Address] as? String) ?: throw WrongParameters()
            val coinValue: CoinValue = (params[SendModule.AdapterFields.CoinValue] as? CoinValue) ?: throw WrongParameters()
            val feePriority = params[SendModule.AdapterFields.FeeRatePriority] as? FeeRatePriority ?: FeeRatePriority.MEDIUM
            interactor.send(address, coinValue.value, feePriority)
        } catch (error: WrongParameters){
            //wrong parameters exception
        }
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
            is WrongParameters -> R.string.Error
            is UnknownHostException -> R.string.Hud_Text_NoInternet
            else -> R.string.Hud_Network_Issue
        }
    }

}
