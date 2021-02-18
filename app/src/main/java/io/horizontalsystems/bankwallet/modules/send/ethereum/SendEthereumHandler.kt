package io.horizontalsystems.bankwallet.modules.send.ethereum

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.NoFeeSendTransactionError
import io.horizontalsystems.bankwallet.entities.FeeRateState
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
        private val interactor: SendModule.ISendEthereumInteractor)
    : SendModule.ISendHandler, SendAmountModule.IAmountModuleDelegate, SendAddressModule.IAddressModuleDelegate, SendFeeModule.IFeeModuleDelegate {

    private var estimateGasLimitState: FeeRateState = FeeRateState.Value(0)
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
                SendModule.Input.Fee,
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
            addressModule.validateAddress()
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

        if (feeModule.feeRateState is FeeRateState.Error) {

            feeModule.setFee(BigDecimal.ZERO)
            processFee((feeModule.feeRateState as FeeRateState.Error).error)

        } else if (estimateGasLimitState is FeeRateState.Error) {

            feeModule.setFee(BigDecimal.ZERO)
            processFee((estimateGasLimitState as FeeRateState.Error).error)

        } else if (feeModule.feeRateState is FeeRateState.Value && estimateGasLimitState is FeeRateState.Value) {

            amountModule.setAvailableBalance(interactor.availableBalance((feeModule.feeRateState as FeeRateState.Value).value, (estimateGasLimitState as FeeRateState.Value).value))

            feeModule.setError(null)
            feeModule.setFee(interactor.fee((feeModule.feeRateState as FeeRateState.Value).value, (estimateGasLimitState as FeeRateState.Value).value))
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

            estimateGasLimitState = FeeRateState.Loading

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

    private fun syncCurrencyAmount() {
        feeModule.setAmountInfo(amountModule.sendAmountInfo)
        syncState()
    }

    override fun confirmationViewItems(): List<SendModule.SendConfirmationViewItem> {
        return listOf(
                SendModule.SendConfirmationAmountViewItem(amountModule.primaryAmountInfo(), amountModule.secondaryAmountInfo(), addressModule.validAddress()),
                SendModule.SendConfirmationFeeViewItem(feeModule.primaryAmountInfo, feeModule.secondaryAmountInfo)
        )
    }

    override fun sendSingle(logger: AppLogger): Single<Unit> {
        val feeRate = feeModule.feeRate
        val gasLimit = estimateGasLimitState

        return when {
            feeRate == null -> Single.error(NoFeeSendTransactionError())
            gasLimit !is FeeRateState.Value -> Single.error(NoFeeSendTransactionError())
            else -> interactor.send(amountModule.validAmount(), addressModule.validAddress().hex, feeRate, gasLimit.value, logger)
        }
    }

    override fun onModulesDidLoad() {
        feeModule.setBalance(interactor.balance)
        feeModule.fetchFeeRate()
        feeModule.setAvailableFeeBalance(interactor.ethereumBalance)

        amountModule.setMinimumRequiredBalance(interactor.minimumRequiredBalance)
        amountModule.setMinimumAmount(interactor.minimumAmount)
        syncState()
        syncEstimateGasLimit()
    }

    override fun onClear() {
        disposable?.dispose()
    }

    // SendAmountModule.IAmountModuleDelegate

    override fun onChangeAmount() {
        syncCurrencyAmount()
        if (syncValidation()) {
            syncEstimateGasLimit()
        }
    }

    override fun onChangeInputType(inputType: SendModule.InputType) {
        feeModule.setInputType(inputType)
    }

    override fun onRateUpdated(rate: BigDecimal?) {
        feeModule.setRate(rate)
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

    private fun onReceiveGasLimit(gasLimit: Long) {
        estimateGasLimitState = FeeRateState.Value(gasLimit)

        syncState()
        syncValidation()
    }

    private fun onGasLimitError(error: Exception) {
        estimateGasLimitState = FeeRateState.Error(error)

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
