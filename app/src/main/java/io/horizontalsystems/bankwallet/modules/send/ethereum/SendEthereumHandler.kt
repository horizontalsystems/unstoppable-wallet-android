package io.horizontalsystems.bankwallet.modules.send.ethereum

import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoModule
import io.reactivex.Single
import java.math.BigDecimal

class SendEthereumHandler(private val interactor: SendModule.ISendEthereumInteractor,
                          private val router: SendModule.IRouter)
    : SendModule.ISendHandler, SendAmountModule.IAmountModuleDelegate, SendAddressModule.IAddressModuleDelegate,
      SendFeeModule.IFeeModuleDelegate {

    private fun syncValidation() {
        try {
            amountModule.validAmount()
            addressModule.validAddress()

            delegate.onChange(true)

        } catch (e: Exception) {
            delegate.onChange(false)
        }
    }

    private fun syncAvailableBalance() {
        amountModule.setAvailableBalance(interactor.availableBalance(gasPrice = feeModule.feeRate))
    }

    private fun syncFee() {
        feeModule.setFee(interactor.fee(feeModule.feeRate))
    }

    // SendModule.ISendHandler

    override lateinit var amountModule: SendAmountModule.IAmountModule
    override lateinit var addressModule: SendAddressModule.IAddressModule
    override lateinit var feeModule: SendFeeModule.IFeeModule
    override lateinit var memoModule: SendMemoModule.IMemoModule
    override var hodlerModule: SendHodlerModule.IHodlerModule? = null

    override lateinit var delegate: SendModule.ISendHandlerDelegate

    override val inputItems: List<SendModule.Input>
        get() = listOf(
                SendModule.Input.Amount,
                SendModule.Input.Address,
                SendModule.Input.Fee(true),
                SendModule.Input.ProceedButton)

    override fun confirmationViewItems(): List<SendModule.SendConfirmationViewItem> {
        return listOf(
                SendModule.SendConfirmationAmountViewItem(amountModule.primaryAmountInfo(),
                                                          amountModule.secondaryAmountInfo(),
                                                          addressModule.validAddress()),
                SendModule.SendConfirmationFeeViewItem(feeModule.primaryAmountInfo, feeModule.secondaryAmountInfo),
                SendModule.SendConfirmationDurationViewItem(feeModule.duration))
    }

    override fun sendSingle(): Single<Unit> {
        return interactor.send(amountModule.validAmount(), addressModule.validAddress(), feeModule.feeRate)
    }

    override fun onModulesDidLoad() {
        amountModule.setMinimumRequiredBalance(interactor.minimumRequiredBalance)
        syncAvailableBalance()
        feeModule.setAvailableFeeBalance(interactor.ethereumBalance)
        syncFee()
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

    // SendFeeModule.IFeeModuleDelegate

    override fun onUpdateFeeRate(feeRate: Long) {
        syncAvailableBalance()
        syncFee()
        syncValidation()
    }

}
