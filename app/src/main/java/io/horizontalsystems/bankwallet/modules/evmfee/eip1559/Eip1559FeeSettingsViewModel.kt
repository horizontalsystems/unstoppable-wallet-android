package io.horizontalsystems.bankwallet.modules.evmfee.eip1559

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.feePriceScale
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.FeePriceScale
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.evmfee.*
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.disposables.CompositeDisposable

class Eip1559FeeSettingsViewModel(
    private val gasPriceService: Eip1559GasPriceService,
    feeService: IEvmFeeService,
    private val coinService: EvmCoinService,
    private val cautionViewItemFactory: CautionViewItemFactory
) : ViewModel() {

    private val scale = coinService.token.blockchainType.feePriceScale
    private val disposable = CompositeDisposable()

    var feeSummaryViewItem by mutableStateOf<FeeSummaryViewItem?>(null)
        private set

    var currentBaseFee by mutableStateOf<String?>(null)
        private set

    var baseFeeViewItem by mutableStateOf<FeeViewItem?>(null)
        private set

    var priorityFeeViewItem by mutableStateOf<FeeViewItem?>(null)
        private set

    var cautions by mutableStateOf<List<CautionViewItem>>(listOf())
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

    fun onSelectGasPrice(baseFee: Long, maxPriorityFee: Long) {
        gasPriceService.setGasPrice(baseFee, maxPriorityFee)
    }

    fun onIncrementBaseFee(currentBaseFee: Long, currentMaxPriorityFee: Long) {
        gasPriceService.setGasPrice(currentBaseFee + scale.scaleValue, currentMaxPriorityFee)
    }

    fun onDecrementBaseFee(currentBaseFee: Long, currentMaxPriorityFee: Long) {
        gasPriceService.setGasPrice((currentBaseFee - scale.scaleValue).coerceAtLeast(0), currentMaxPriorityFee)
    }

    fun onIncrementPriorityFee(currentBaseFee: Long, currentMaxPriorityFee: Long) {
        gasPriceService.setGasPrice(currentBaseFee, currentMaxPriorityFee + scale.scaleValue)
    }

    fun onDecrementPriorityFee(currentBaseFee: Long, currentMaxPriorityFee: Long) {
        gasPriceService.setGasPrice(currentBaseFee, (currentMaxPriorityFee - scale.scaleValue).coerceAtLeast(0))
    }

    override fun onCleared() {
        disposable.clear()
    }

    private fun sync(state: DataState<GasPriceInfo>) {
        sync(gasPriceService.currentBaseFee)

        state.dataOrNull?.let { gasPriceInfo ->
            if (gasPriceInfo.gasPrice is GasPrice.Eip1559) {
                baseFeeViewItem = FeeViewItem(
                    weiValue = gasPriceInfo.gasPrice.maxFeePerGas - gasPriceInfo.gasPrice.maxPriorityFeePerGas,
                    scale = scale
                )
                priorityFeeViewItem = FeeViewItem(
                    weiValue = gasPriceInfo.gasPrice.maxPriorityFeePerGas,
                    scale = scale
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
        syncCautions(transactionStatus)
    }

    private fun syncFeeViewItems(transactionStatus: DataState<Transaction>) {
        val notAvailable = Translator.getString(R.string.NotAvailable)
        when (transactionStatus) {
            DataState.Loading -> {
                feeSummaryViewItem = FeeSummaryViewItem(notAvailable, notAvailable, ViewState.Loading)
            }
            is DataState.Error -> {
                feeSummaryViewItem = FeeSummaryViewItem(notAvailable, notAvailable, ViewState.Error(transactionStatus.error))
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data
                val viewState = transaction.errors.firstOrNull()?.let { ViewState.Error(it) } ?: ViewState.Success
                val fee = coinService.amountData(transactionStatus.data.gasData.fee).getFormatted()
                val gasLimit = App.numberFormatter.format(transactionStatus.data.gasData.gasLimit.toBigDecimal(), 0, 0)

                feeSummaryViewItem = FeeSummaryViewItem(fee, gasLimit, viewState)
            }
        }
    }

    private fun syncCautions(transactionStatus: DataState<Transaction>) {
        val warnings = mutableListOf<Warning>()
        val errors = mutableListOf<Throwable>()

        if (transactionStatus is DataState.Error) {
            errors.add(transactionStatus.error)
        } else if (transactionStatus is DataState.Success) {
            warnings.addAll(transactionStatus.data.warnings)
            errors.addAll(transactionStatus.data.errors)
        }

        cautions = cautionViewItemFactory.cautionViewItems(warnings, errors)
    }

}
