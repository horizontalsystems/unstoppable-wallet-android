package io.horizontalsystems.bankwallet.modules.send.binance

/*
class SendBinancePresenter(private val interactor: SendModule.ISendBinanceInteractor,
                           private val router: SendModule.IRouter,
                           private val confirmationFactory: SendConfirmationViewItemFactory) : SendModule.IViewDelegate, SendModule.ISendBinanceInteractorDelegate,
        SendAmountModule.IAmountModuleDelegate,
        SendAddressModule.IAddressModuleDelegate {

    var view: SendModule.IView? = null

    private fun syncSendButton() {
        view?.setSendButtonEnabled(amountModule.validAmount != null && addressModule.currentAddress != null && feeModule.isValid)
    }

    // SendModule.IViewDelegate

    override lateinit var amountModule: SendAmountModule.IAmountModule

    override lateinit var addressModule: SendAddressModule.IAddressModule

    override lateinit var feeModule: SendFeeModule.IFeeModule

    override fun onViewDidLoad() {
        view?.loadInputItems(listOf(
                SendModule.Input.Amount,
                SendModule.Input.Address,
                SendModule.Input.Fee(false),
                SendModule.Input.ProceedButton))
    }

    override fun onModulesDidLoad() {
        amountModule.setAvailableBalance(interactor.availableBalance)

        feeModule.setFee(interactor.fee)
        feeModule.setAvailableFeeBalance(interactor.availableBinanceBalance)
    }

    override fun onAddressScan(address: String) {
        addressModule.didScanQrCode(address)
    }

    override fun onProceedClicked() {
        val inputType = amountModule.inputType
        val address = addressModule.currentAddress ?: return
        val coinValue = amountModule.coinAmount
        val currencyValue = amountModule.fiatAmount
        val feeCoinValue = feeModule.coinValue
        val feeCurrencyValue = feeModule.currencyValue
        val duration = feeModule.duration

        val confirmationViewItem = confirmationFactory.confirmationViewItem(
                inputType,
                address,
                coinValue,
                currencyValue,
                feeCoinValue,
                feeCurrencyValue,
                duration,
                false)

        view?.showConfirmation(confirmationViewItem)
    }

    override fun onSendConfirmed(memo: String?) {
        val amount = amountModule.validAmount ?: return
        val address = addressModule.currentAddress ?: return

        interactor.send(amount, address, memo)
    }

    override fun onClear() {
        interactor.clear()
    }

    // SendModule.ISendBinanceInteractorDelegate

    override fun didSend() {
        router.closeWithSuccess()
    }

    override fun didFailToSend(error: Throwable) {
        view?.showErrorInToast(error)
    }

    // SendAmountModule.IAmountModuleDelegate

    override fun onChangeAmount() {
        syncSendButton()
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {
        feeModule.setInputType(inputType)
    }

    // SendAddressModule.IAddressModuleDelegate

    override fun validate(address: String) {
        interactor.validate(address)
    }

    override fun onUpdateAddress() {
        syncSendButton()
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAmount(amount)
    }

    override fun scanQrCode() {
        router.scanQrCode()
    }

}
*/
