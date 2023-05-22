package io.horizontalsystems.bankwallet.modules.evmfee.eip1559

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.feePriceScale
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.FeePriceScale
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSummaryViewItem
import io.horizontalsystems.bankwallet.modules.evmfee.FeeViewItem
import io.horizontalsystems.bankwallet.modules.evmfee.GasPriceInfo
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmFeeService
import io.horizontalsystems.bankwallet.modules.evmfee.Transaction
import io.horizontalsystems.bankwallet.modules.fee.FeeItem
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.disposables.CompositeDisposable

class Eip1559FeeSettingsViewModel(
    private val gasPriceService: Eip1559GasPriceService,
    feeService: IEvmFeeService,
    private val coinService: EvmCoinService
) : ViewModel() {

    private val scale = coinService.token.blockchainType.feePriceScale
    private val disposable = CompositeDisposable()

    var feeSummaryViewItem by mutableStateOf<FeeSummaryViewItem?>(null)
        private set

    var currentBaseFee by mutableStateOf<String?>(null)
        private set

    var maxFeeViewItem by mutableStateOf<FeeViewItem?>(null)
        private set

    var priorityFeeViewItem by mutableStateOf<FeeViewItem?>(null)
        private set

    init {
        sync(gasPriceService.state)
        gasPriceService.stateObservable
            .subscribeIO {
                sync(it)
            }.let {
                disposable.add(it)
            }

        syncTransactionStatus(feeService.transactionStatus)
        feeService.transactionStatusObservable
            .subscribe { transactionStatus ->
                syncTransactionStatus(transactionStatus)
            }
            .let {
                disposable.add(it)
            }
    }

    fun onSelectGasPrice(maxFee: Long, priorityFee: Long) {
        gasPriceService.setGasPrice(maxFee, priorityFee)
    }

    fun onIncrementMaxFee(maxFee: Long, priorityFee: Long) {
        gasPriceService.setGasPrice(maxFee + scale.scaleValue, priorityFee)
    }

    fun onDecrementMaxFee(maxFee: Long, priorityFee: Long) {
        gasPriceService.setGasPrice((maxFee - scale.scaleValue).coerceAtLeast(0), priorityFee)
    }

    fun onIncrementPriorityFee(maxFee: Long, priorityFee: Long) {
        gasPriceService.setGasPrice(maxFee, priorityFee + scale.scaleValue)
    }

    fun onDecrementPriorityFee(maxFee: Long, priorityFee: Long) {
        gasPriceService.setGasPrice(maxFee, (priorityFee - scale.scaleValue).coerceAtLeast(0))
    }

    override fun onCleared() {
        disposable.clear()
    }

    private fun sync(state: DataState<GasPriceInfo>) {
        sync(gasPriceService.currentBaseFee)

        state.dataOrNull?.let { gasPriceInfo ->
            if (gasPriceInfo.gasPrice is GasPrice.Eip1559) {
                maxFeeViewItem = FeeViewItem(
                    weiValue = gasPriceInfo.gasPrice.maxFeePerGas,
                    scale = scale,
                    warnings = gasPriceInfo.warnings,
                    errors = gasPriceInfo.errors
                )
                priorityFeeViewItem = FeeViewItem(
                    weiValue = gasPriceInfo.gasPrice.maxPriorityFeePerGas,
                    scale = scale,
                    warnings = gasPriceInfo.warnings,
                    errors = gasPriceInfo.errors
                )
            }
        }
    }

    private fun scaledString(wei: Long, scale: FeePriceScale): String {
        val gwei = wei.toDouble() / scale.scaleValue
        return "${gwei.toBigDecimal().toPlainString()} ${scale.unit}"
    }

    private fun sync(baseFee: Long?) {
        currentBaseFee = if (baseFee != null) {
            scaledString(baseFee, coinService.token.blockchainType.feePriceScale)
        } else {
            Translator.getString(R.string.NotAvailable)
        }
    }

    private fun syncTransactionStatus(transactionStatus: DataState<Transaction>) {
        syncFeeViewItems(transactionStatus)
    }

    private fun syncFeeViewItems(transactionStatus: DataState<Transaction>) {
        val notAvailable = Translator.getString(R.string.NotAvailable)
        when (transactionStatus) {
            DataState.Loading -> {
                feeSummaryViewItem = FeeSummaryViewItem(null, notAvailable, ViewState.Loading)
            }
            is DataState.Error -> {
                feeSummaryViewItem = FeeSummaryViewItem(null, notAvailable, ViewState.Error(transactionStatus.error))
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data
                val viewState = transaction.errors.firstOrNull()?.let { ViewState.Error(it) } ?: ViewState.Success
                val feeAmountData = coinService.amountData(transactionStatus.data.gasData.estimatedFee, transactionStatus.data.gasData.isSurcharged)
                val feeItem = FeeItem(feeAmountData.primary.getFormattedPlain(), feeAmountData.secondary?.getFormattedPlain())
                val gasLimit = App.numberFormatter.format(transactionStatus.data.gasData.gasLimit.toBigDecimal(), 0, 0)

                feeSummaryViewItem = FeeSummaryViewItem(feeItem, gasLimit, viewState)
            }
        }
    }
}
