package io.horizontalsystems.bankwallet.modules.send.binance

import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.reactivex.Single
import java.math.BigDecimal

class SendBinanceHandler(private val interactor: SendModule.ISendBinanceInteractor,
                         private val router: SendModule.IRouter) : SendModule.ISendHandler,
        SendAmountModule.IAmountModuleDelegate,
        SendAddressModule.IAddressModuleDelegate {

    private fun syncValidation() {
        try {
            amountModule.validAmount()
            addressModule.validAddress()

            delegate.onChange(true)

        } catch (e: Exception) {
            delegate.onChange(false)
        }
    }

    // SendModule.ISendHandler

    override lateinit var amountModule: SendAmountModule.IAmountModule

    override lateinit var addressModule: SendAddressModule.IAddressModule

    override lateinit var feeModule: SendFeeModule.IFeeModule

    override val inputItems: List<SendModule.Input> = listOf(
            SendModule.Input.Amount,
            SendModule.Input.Address,
            SendModule.Input.Fee(false),
            SendModule.Input.ProceedButton)

    override lateinit var delegate: SendModule.ISendHandlerDelegate

    override fun confirmationViewItems(): List<SendModule.SendConfirmationViewItem> {
        TODO("not implemented")
    }

    override fun sendSingle(): Single<Unit> {
        return interactor.send(amountModule.validAmount(), addressModule.validAddress(), null)
    }

    override fun onModulesDidLoad() {
        amountModule.setAvailableBalance(interactor.availableBalance)

        feeModule.setFee(interactor.fee)
        feeModule.setAvailableFeeBalance(interactor.availableBinanceBalance)
    }

    override fun onAddressScan(address: String) {
        addressModule.didScanQrCode(address)
    }

    // SendAmountModule.IAmountModuleDelegate

    override fun onChangeAmount() {
        syncValidation()
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {
        feeModule.setInputType(inputType)
    }

    // SendAddressModule.IAddressModuleDelegate

    override fun validate(address: String) {
        interactor.validate(address)
    }

    override fun onUpdateAddress() {
        syncValidation()
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAmount(amount)
    }

    override fun scanQrCode() {
        router.scanQrCode()
    }

}
