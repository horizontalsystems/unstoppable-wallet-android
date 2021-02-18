package io.horizontalsystems.bankwallet.modules.send.zcash

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoModule
import io.reactivex.Single
import java.math.BigDecimal

class SendZcashHandler(
        private val interactor: SendModule.ISendZcashInteractor
) : SendModule.ISendHandler, SendAmountModule.IAmountModuleDelegate, SendAddressModule.IAddressModuleDelegate, SendFeeModule.IFeeModuleDelegate {

    private fun syncValidation() {
        var amountError: Throwable? = null
        var addressError: Throwable? = null

        try {
            amountModule.validAmount()
        } catch (e: Exception) {
            amountError = e
        }

        try {
            addressModule.validateAddress()
        } catch (e: Exception) {
            addressError = e
        }

        delegate.onChange(amountError == null && addressError == null && feeModule.isValid, amountError, addressError)
    }

    private fun syncAvailableBalance() {
        amountModule.setAvailableBalance(interactor.availableBalance)
    }

    //region SendModule.ISendHandler
    override lateinit var amountModule: SendAmountModule.IAmountModule
    override lateinit var addressModule: SendAddressModule.IAddressModule
    override lateinit var feeModule: SendFeeModule.IFeeModule
    override lateinit var memoModule: SendMemoModule.IMemoModule
    override var hodlerModule: SendHodlerModule.IHodlerModule? = null

    override lateinit var delegate: SendModule.ISendHandlerDelegate
    override fun sync() {}

    override val inputItems: List<SendModule.Input> = listOf(
            SendModule.Input.Amount,
            SendModule.Input.Address(true),
            SendModule.Input.Memo(120),
            SendModule.Input.Fee,
            SendModule.Input.ProceedButton
    )

    override fun confirmationViewItems(): List<SendModule.SendConfirmationViewItem> {
        return listOf(
                SendModule.SendConfirmationAmountViewItem(
                        amountModule.primaryAmountInfo(),
                        amountModule.secondaryAmountInfo(),
                        addressModule.validAddress(),
                ),
                SendModule.SendConfirmationMemoViewItem(memoModule.memo),
                SendModule.SendConfirmationFeeViewItem(feeModule.primaryAmountInfo, feeModule.secondaryAmountInfo)
        )
    }

    override fun sendSingle(logger: AppLogger): Single<Unit> {
        return interactor.send(amountModule.validAmount(), addressModule.validAddress().hex, memoModule.memo, logger)
    }

    override fun onModulesDidLoad() {
        syncAvailableBalance()
        feeModule.setFee(interactor.fee)
    }

    //endregion

    //region SendAmountModule.IAmountModuleDelegate
    override fun onChangeAmount() {
        syncValidation()
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {

    }
    //endregion

    //region SendAddressModule.IAddressModuleDelegate
    override fun validate(address: String) {
        interactor.validate(address)
    }

    override fun onUpdateAddress() {
        syncValidation()
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAvailableBalance(amount)
    }

    //endregion

    //region SendFeeModule.IFeeModuleDelegate
    override fun onUpdateFeeRate() {
    }
    //endregion

}
