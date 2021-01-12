package io.horizontalsystems.bankwallet.modules.send.ethereum

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.entities.FeeState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.SendFeeModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SendHodlerModule
import io.horizontalsystems.bankwallet.modules.send.submodules.memo.SendMemoModule
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal


class SendEthereumHandler(
        private val interactor: SendModule.ISendEthereumInteractor,
        private val router: SendModule.IRouter)
    : SendModule.ISendHandler, SendAmountModule.IAmountModuleDelegate, SendAddressModule.IAddressModuleDelegate, SendFeeModule.IFeeModuleDelegate {

    private var estimateGasLimitState: FeeState = FeeState.Value(0)
    private var disposable: Disposable? = null

    override lateinit var amountModule: SendAmountModule.IAmountModule
    override lateinit var addressModule: SendAddressModule.IAddressModule
    override lateinit var feeModule: SendFeeModule.IFeeModule
    override lateinit var memoModule: SendMemoModule.IMemoModule

    override var hodlerModule: SendHodlerModule.IHodlerModule? = null

    override lateinit var delegate: SendModule.ISendHandlerDelegate

    override val inputItems: List<SendModule.Input>
        get() = listOf(
                SendModule.Input.Amount,
                SendModule.Input.Address(),
                SendModule.Input.Fee(true),
                SendModule.Input.ProceedButton)

    private fun syncValidation(): Boolean {
        var amountError: Throwable? = null
        var addressError: Throwable? = null

        try {
            amountModule.validAmount()
        } catch (e: Exception) {
            amountError = e
        }

        try {
            addressModule.validAddress()
        } catch (e: Exception) {
            addressError = e
        }

        val isValid = feeModule.isValid && feeModule.feeRateState.isValid && estimateGasLimitState.isValid
                && amountError == null && addressError == null

        delegate.onChange(isValid, amountError, addressError)

        return isValid
    }

    private fun syncState() {

        val loading = feeModule.feeRateState.isLoading || estimateGasLimitState.isLoading

        amountModule.setLoading(loading)
        feeModule.setLoading(loading)

        if (loading)
            return

        if (feeModule.feeRateState is FeeState.Error) {

            feeModule.setFee(BigDecimal.ZERO)
            processFee((feeModule.feeRateState as FeeState.Error).error)

        } else if (estimateGasLimitState is FeeState.Error) {

            feeModule.setFee(BigDecimal.ZERO)
            processFee((estimateGasLimitState as FeeState.Error).error)

        } else if (feeModule.feeRateState is FeeState.Value && estimateGasLimitState is FeeState.Value) {

            amountModule.setAvailableBalance(interactor.availableBalance((feeModule.feeRateState as FeeState.Value).value, (estimateGasLimitState as FeeState.Value).value))

            feeModule.setError(null)
            feeModule.setFee(interactor.fee((feeModule.feeRateState as FeeState.Value).value, (estimateGasLimitState as FeeState.Value).value))
        }
    }

    private fun processFee(error: Exception) {
        feeModule.setError(error)
    }

    private fun syncEstimateGasLimit() {
        try {
            val amount = amountModule.currentAmount
            val address = try {
                addressModule.validAddress()
            } catch (ex: Throwable) {
                null
            }

            estimateGasLimitState = FeeState.Loading

            syncState()

            disposable?.dispose()
            disposable = interactor.estimateGasLimit(address?.hex, amount, feeModule.feeRate)
                    .subscribeOn(Schedulers.io())
                    .subscribe({ gasLimit ->
                        onReceiveGasLimit(gasLimit)
                    }, { error ->
                        onGasLimitError(error as Exception)
                    })


        } catch (e: Exception) {
            onReceiveGasLimit(0)
        }
    }

    override fun confirmationViewItems(): List<SendModule.SendConfirmationViewItem> {
        return listOf(
                SendModule.SendConfirmationAmountViewItem(amountModule.primaryAmountInfo(), amountModule.secondaryAmountInfo(), addressModule.validAddress().hex),
                SendModule.SendConfirmationFeeViewItem(feeModule.primaryAmountInfo, feeModule.secondaryAmountInfo),
                SendModule.SendConfirmationDurationViewItem(feeModule.duration))
    }

    override fun sendSingle(logger: AppLogger): Single<Unit> {
        val gasLimit = estimateGasLimitState
        if (gasLimit !is FeeState.Value) {
            throw Exception("SendTransactionError.unknown")
        }

        return interactor.send(amountModule.validAmount(), addressModule.validAddress().hex, feeModule.feeRate, gasLimit.value, logger)
    }

    override fun onModulesDidLoad() {
        feeModule.fetchFeeRate()
        amountModule.setMinimumRequiredBalance(interactor.minimumRequiredBalance)
        amountModule.setMinimumAmount(interactor.minimumAmount)
        feeModule.setAvailableFeeBalance(interactor.ethereumBalance)
        syncState()
        syncEstimateGasLimit()
    }

    override fun onAddressScan(address: String) {
    }

    override fun onClear() {
        disposable?.dispose()
    }

    // SendAmountModule.IAmountModuleDelegate

    override fun onChangeAmount() {
        if (syncValidation()) {
            syncEstimateGasLimit()
        }
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {
        feeModule.setInputType(inputType)
    }

    // SendAddressModule.IAddressModuleDelegate

    override fun validate(address: String) {
        interactor.validate(address)
    }

    override fun onUpdateAddress() {
        if (syncValidation()) {
            syncEstimateGasLimit()
        }
    }

    override fun onUpdateAmount(amount: BigDecimal) {
        amountModule.setAmount(amount)
    }

    override fun scanQrCode() {
        router.scanQrCode()
    }

    private fun onReceiveGasLimit(gasLimit: Long) {
        estimateGasLimitState = FeeState.Value(gasLimit)

        syncState()
        syncValidation()
    }

    private fun onGasLimitError(error: Exception) {
        estimateGasLimitState = FeeState.Error(error)

        syncState()
        syncValidation()
    }

    override fun sync() {
        if (feeModule.feeRateState.isError || estimateGasLimitState.isError) {
            feeModule.fetchFeeRate()
            syncEstimateGasLimit()
        }
    }

    // SendFeeModule.IFeeModuleDelegate
    override fun onUpdateFeeRate() {
        syncState()
        if (syncValidation()) {
            syncEstimateGasLimit()
        }
    }
}
