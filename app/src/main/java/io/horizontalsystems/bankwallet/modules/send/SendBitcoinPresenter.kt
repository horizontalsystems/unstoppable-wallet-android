package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.WrongParameters
import io.horizontalsystems.bankwallet.modules.send.sendviews.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.sendviews.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.sendviews.fee.SendFeeModule
import java.math.BigDecimal
import java.net.UnknownHostException

class SendBitcoinPresenter(private val interactor: SendModule.ISendBitcoinInteractor,
                           private val router: SendModule.IRouter,
                           private val confirmationFactory: SendConfirmationViewItemFactory) : SendModule.IViewDelegate, SendModule.ISendBitcoinInteractorDelegate,
        SendAmountModule.IAmountModuleDelegate,
        SendAddressModule.IAddressModuleDelegate,
        SendFeeModule.IFeeModuleDelegate {

    override lateinit var amountModule: SendAmountModule.IAmountModule
    override lateinit var addressModule: SendAddressModule.IAddressModule
    override lateinit var feeModule: SendFeeModule.IFeeModule

    var view: SendModule.IView? = null

    private fun syncSendButton() {
        view?.setSendButtonEnabled(enabled = amountModule.validAmount != null && addressModule.address != null)
    }

    private fun syncAvailableBalance() {
        interactor.fetchAvailableBalance(feeModule.feeRate, addressModule.address)
    }

    private fun syncFee() {
        interactor.fetchFee(amountModule.coinAmount.value, feeModule.feeRate, addressModule.address)
    }

    private fun getErrorText(error: Throwable): Int {
        return when (error) {
            is WrongParameters -> R.string.Error
            is UnknownHostException -> R.string.Hud_Text_NoInternet
            else -> R.string.Hud_Network_Issue
        }
    }

    // SendModule.IViewDelegate

    override fun onViewDidLoad() {
        view?.loadInputItems(listOf(
                SendModule.Input.Amount,
                SendModule.Input.Address,
                SendModule.Input.Fee(true),
                SendModule.Input.SendButton))
    }

    override fun onModulesDidLoad() {
        syncAvailableBalance()
    }

    override fun onAddressScan(address: String) {
        addressModule.didScanQrCode(address)
    }

    override fun onSendClicked() {
        val inputType = amountModule.inputType
        val address = addressModule.address ?: return
        val coinValue = amountModule.coinAmount
        val currencyValue = amountModule.fiatAmount
        val feeCoinValue = feeModule.coinValue
        val feeCurrencyValue = feeModule.currencyValue

        val confirmationViewItem = confirmationFactory.confirmationViewItem(
                inputType,
                address,
                coinValue,
                currencyValue,
                feeCoinValue,
                feeCurrencyValue,
                false
        )

        view?.showConfirmation(confirmationViewItem)
    }

    override fun onSendConfirmed(memo: String?) {
        val amount = amountModule.validAmount ?: return
        val address = addressModule.address ?: return
        val feeRate = feeModule.feeRate

        interactor.send(amount, address, feeRate)
    }

    override fun onClear() {
        interactor.clear()
    }

    // SendModule.ISendBitcoinInteractorDelegate

    override fun didSend() {
        view?.dismissWithSuccess()
    }

    override fun didFailToSend(error: Throwable) {
        view?.showError(getErrorText(error))
    }

    override fun didFetchAvailableBalance(availableBalance: BigDecimal) {
        amountModule.setAvailableBalance(availableBalance)
        syncSendButton()
    }

    override fun didFetchFee(fee: BigDecimal) {
        feeModule.setFee(fee)
    }

    // SendAmountModule.ModuleDelegate

    override fun onChangeAmount() {
        syncFee()
        syncSendButton()
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {
        feeModule.setInputType(inputType)
    }

    // SendAddressModule.ModuleDelegate

    override fun validate(address: String) {
        interactor.validate(address)
    }

    override fun onUpdateAddress() {
        syncAvailableBalance()
        syncFee()
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAmount(amount)
    }

    override fun scanQrCode() {
        router.scanQrCode()
    }

    // SendFeeModule.IFeeModuleDelegate

    override fun onFeePriorityChange(feeRatePriority: FeeRatePriority) {
        syncAvailableBalance()
        syncFee()
    }

}
