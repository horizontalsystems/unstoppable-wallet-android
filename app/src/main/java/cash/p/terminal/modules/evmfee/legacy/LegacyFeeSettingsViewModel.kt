package cash.p.terminal.modules.evmfee.legacy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.Warning
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.ethereum.CautionViewItemFactory
import cash.p.terminal.core.ethereum.EvmCoinService
import cash.p.terminal.core.feePriceScale
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.evmfee.*
import io.reactivex.disposables.CompositeDisposable

class LegacyFeeSettingsViewModel(
    private val gasPriceService: LegacyGasPriceService,
    private val feeService: IEvmFeeService,
    private val coinService: EvmCoinService,
    private val cautionViewItemFactory: CautionViewItemFactory
) : ViewModel() {

    private val scale = coinService.token.blockchainType.feePriceScale
    private val disposable = CompositeDisposable()

    var feeSummaryViewItem by mutableStateOf<FeeSummaryViewItem?>(null)
        private set

    var feeViewItem by mutableStateOf<FeeViewItem?>(null)
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

    fun onSelectGasPrice(gasPrice: Long) {
        gasPriceService.setGasPrice(gasPrice)
    }

    fun onIncrementGasPrice(currentWeiValue: Long) {
        gasPriceService.setGasPrice(currentWeiValue + scale.scaleValue)
    }

    fun onDecrementGasPrice(currentWeiValue: Long) {
        gasPriceService.setGasPrice((currentWeiValue - scale.scaleValue).coerceAtLeast(0))
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

    private fun sync(state: DataState<GasPriceInfo>) {
        if (state is DataState.Success) {
            feeViewItem = FeeViewItem(weiValue = state.data.gasPrice.max, scale = scale)
        }
    }

}
