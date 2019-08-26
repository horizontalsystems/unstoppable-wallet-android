package io.horizontalsystems.bankwallet.modules.send.eos

import io.horizontalsystems.bankwallet.core.CoinException
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationViewItemFactory
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import java.math.BigDecimal

class SendEosPresenter(private val interactor: SendModule.ISendEosInteractor,
                       private val router: SendModule.IRouter,
                       private val confirmationFactory: SendConfirmationViewItemFactory) : SendModule.IViewDelegate, SendModule.ISendEosInteractorDelegate,
        SendAmountModule.IAmountModuleDelegate,
        SendAddressModule.IAddressModuleDelegate {

    var view: SendModule.IView? = null

    private fun syncSendButton() {
        view?.setSendButtonEnabled(enabled = amountModule.validAmount != null && addressModule.address != null)
    }

    private fun syncAvailableBalance() {
        amountModule.setAvailableBalance(interactor.availableBalance)
    }

    // SendModule.IViewDelegate

    override lateinit var amountModule: SendAmountModule.IAmountModule

    override lateinit var addressModule: SendAddressModule.IAddressModule

    override lateinit var feeModule: SendFeeModule.IFeeModule

    override fun onViewDidLoad() {
        view?.loadInputItems(listOf(
                SendModule.Input.Amount,
                SendModule.Input.Address,
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

        val confirmationViewItem = confirmationFactory.confirmationViewItem(
                inputType,
                address,
                coinValue,
                currencyValue,
                null,
                null,
                null,
                true)

        view?.showConfirmation(confirmationViewItem)
    }

    override fun onSendConfirmed(memo: String?) {
        val amount = amountModule.validAmount ?: return
        val address = addressModule.address ?: return

        interactor.send(amount, address, memo)
    }

    override fun onClear() {
        interactor.clear()
    }

    // SendModule.ISendEthereumInteractorDelegate

    override fun didSend() {
        router.closeWithSuccess()
    }

    override fun didFailToSend(error: Throwable) {
        view?.showErrorInToast(error)
    }

    override fun didFailToSendWithEosBackendError(coinException: CoinException) {
        view?.showErrorInDialog(coinException)
    }

    // SendAmountModule.IAmountModuleDelegate

    override fun onChangeAmount() {
        syncSendButton()
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {

    }

    // SendAddressModule.IAddressModuleDelegate

    override fun validate(address: String) {
        interactor.validate(address)
    }

    override fun onUpdateAddress() {
        syncSendButton()
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAvailableBalance(amount)
    }

    override fun scanQrCode() {
        router.scanQrCode()
    }

}
