package io.horizontalsystems.bankwallet.modules.send

//open class SendPresenter(
//        protected val interactor: SendModule.IInteractor,
//        protected val confirmationFactory: SendConfirmationViewItemFactory)
//    : SendModule.IViewDelegate, SendModule.IInteractorDelegate {
//
//    var view: SendViewModel? = null
//    protected var inputParams: Map<SendModule.AdapterFields, Any?>? = null
//
//    override fun onGetAvailableBalance() {
//        view?.getParamsForAction(SendModule.ParamsAction.AvailableBalance)
//    }
//
//    open val inputs = listOf(
//            SendModule.Input.Amount,
//            SendModule.Input.Address,
//            SendModule.Input.Fee(true),
//            SendModule.Input.SendButton)
//
//    override fun onViewDidLoad() {
//        view?.loadInputItems(inputs)
//    }
//
//    override fun onAmountChanged(coinAmount: BigDecimal?) {
//        coinAmount?.let {
//            updateModules()
//        }
//    }
//
//    override fun onParamsFetchedForAction(params: Map<SendModule.AdapterFields, Any?>, paramsAction: SendModule.ParamsAction) {
//        when (paramsAction) {
//            SendModule.ParamsAction.UpdateModules -> {
//                val updatedParams = params.toMutableMap()
//                val coinValue = (params[SendModule.AdapterFields.CoinValue] as? CoinValue)
//                updatedParams[SendModule.AdapterFields.CoinAmountInBigDecimal] = coinValue?.value
//                        ?: BigDecimal.ZERO
//
//                interactor.validate(updatedParams)
//                interactor.updateFee(updatedParams)
//            }
//            SendModule.ParamsAction.AvailableBalance -> getAvailableBalance(params)
//            SendModule.ParamsAction.ShowConfirm -> showConfirmationDialog(params)
//        }
//    }
//
//    override fun onAddressChanged() {
//        updateModules()
//    }
//
//    override fun parseAddress(address: String) {
//        val parsedAddress = interactor.parsePaymentAddress(address)
//        view?.onAddressParsed(parsedAddress)
//    }
//
//    override fun onValidationComplete(errorList: List<SendStateError>) {
//        var amountValidationSuccess = true
//        errorList.forEach { error ->
//            when (error) {
//                is SendStateError.InsufficientAmount -> {
//                    amountValidationSuccess = false
//                    view?.onValidationError(error)
//                }
//                is SendStateError.InsufficientFeeBalance -> {
//                    view?.onInsufficientFeeBalance(error.fee)
//                }
//            }
//        }
//        if (amountValidationSuccess) {
//            view?.onAmountValidationSuccess()
//        }
//
//        view?.getValidStatesFromModules()
//    }
//
//    override fun onFeeUpdated(fee: BigDecimal) {
//        view?.onFeeUpdated(fee)
//    }
//
//    override fun onSendClicked() {
//        view?.getParamsForAction(SendModule.ParamsAction.ShowConfirm)
//    }
//
//    override fun onConfirmClicked() {
//        view?.getParamsForAction(SendModule.ParamsAction.Send)
//    }
//
//    override fun onClear() {
//        interactor.clear()
//    }
//
//    override fun didSend() {
//        view?.dismissWithSuccess()
//    }
//
//    override fun showError(error: Throwable) {
//        val textResourceId = getErrorText(error)
//        view?.showError(textResourceId)
//    }
//
//    override fun onFeePriorityChange(feeRatePriority: FeeRatePriority) {
//        updateModules()
//    }
//
//    override fun onValidStatesFetchedFromModules(validStates: MutableList<Boolean>) {
//        val invalid = validStates.contains(false)
//        view?.setSendButtonEnabled(!invalid)
//    }
//
//    override fun send(memo: String?) {
//        val mutableMap = inputParams?.toMutableMap()
//        mutableMap?.let { params ->
//            memo?.let {
//                params[SendModule.AdapterFields.Memo] = it
//            }
//            interactor.send(params)
//        }
//    }
//
//    open fun showConfirmationDialog(params: Map<SendModule.AdapterFields, Any?>) {
//        try {
//            val inputType: SendModule.InputType = params[SendModule.AdapterFields.InputType] as SendModule.InputType
//            val address: String = (params[SendModule.AdapterFields.Address] as? String)
//                    ?: throw WrongParameters()
//            val coinValue: CoinValue = (params[SendModule.AdapterFields.CoinValue] as? CoinValue)
//                    ?: throw WrongParameters()
//            val currencyValue: CurrencyValue? = params[SendModule.AdapterFields.CurrencyValue] as? CurrencyValue
//            val feeCoinValue: CoinValue = (params[SendModule.AdapterFields.FeeCoinValue] as? CoinValue)
//                    ?: throw WrongParameters()
//            val feeCurrencyValue: CurrencyValue? = params[SendModule.AdapterFields.FeeCurrencyValue] as? CurrencyValue
//
//            val confirmationViewItem = confirmationFactory.confirmationViewItem(
//                    inputType,
//                    address,
//                    coinValue,
//                    currencyValue,
//                    feeCoinValue,
//                    feeCurrencyValue,
//                    false
//            )
//
//            inputParams = params
//            view?.showConfirmation(confirmationViewItem)
//        } catch (error: WrongParameters) {
//            wrong parameters exception
//        }
//    }
//
//    private fun updateModules() {
//        view?.getParamsForAction(SendModule.ParamsAction.UpdateModules)
//    }
//
//    override fun onInputTypeUpdated(inputType: SendModule.InputType?) {
//        view?.onInputTypeUpdated(inputType)
//    }
//
//    private fun getAvailableBalance(params: Map<SendModule.AdapterFields, Any?>) {
//        var availableBalance = BigDecimal.ZERO
//        try {
//            availableBalance = interactor.getAvailableBalance(params)
//        } catch (e: WrongParameters) {
//            wrong parameters exception
//        }
//
//        view?.onAvailableBalanceRetrieved(availableBalance)
//    }
//
//    private fun getErrorText(error: Throwable): Int {
//        return when (error) {
//            is WrongParameters -> R.string.Error
//            is UnknownHostException -> R.string.Hud_Text_NoInternet
//            else -> R.string.Hud_Network_Issue
//        }
//    }
//
//}
