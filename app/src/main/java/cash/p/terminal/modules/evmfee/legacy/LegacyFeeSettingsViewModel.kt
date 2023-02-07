package cash.p.terminal.modules.evmfee.legacy

import androidx.lifecycle.MutableLiveData
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

    private val disposable = CompositeDisposable()

    val sliderViewItemLiveData = MutableLiveData<SliderViewItem>()
    val feeViewItemLiveData = MutableLiveData<FeeViewItem>()
    val feeViewItemStateLiveData = MutableLiveData<ViewState>()
    val feeViewItemLoadingLiveData = MutableLiveData<Boolean>()

    val cautionsLiveData = MutableLiveData<List<CautionViewItem>>()
    val isRecommendedGasPriceSelected by gasPriceService::isRecommendedGasPriceSelected

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

    fun onClickReset() {
        gasPriceService.setRecommended()
    }

    private fun syncTransactionStatus(transactionStatus: DataState<Transaction>) {
        syncFeeViewItems(transactionStatus)
        syncCautions(transactionStatus)
    }

    private fun syncFeeViewItems(transactionStatus: DataState<Transaction>) {
        when (transactionStatus) {
            DataState.Loading -> {
                feeViewItemLoadingLiveData.postValue(true)
            }
            is DataState.Error -> {
                feeViewItemLoadingLiveData.postValue(false)
                feeViewItemStateLiveData.postValue(ViewState.Error(transactionStatus.error))

                val notAvailable = Translator.getString(R.string.NotAvailable)
                feeViewItemLiveData.postValue(FeeViewItem(notAvailable, notAvailable))
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data
                feeViewItemLoadingLiveData.postValue(false)

                val viewState = transaction.errors.firstOrNull()?.let { ViewState.Error(it) } ?: ViewState.Success
                feeViewItemStateLiveData.postValue(viewState)

                val fee = coinService.amountData(transactionStatus.data.gasData.fee).getFormatted()
                val gasLimit = App.numberFormatter.format(transactionStatus.data.gasData.gasLimit.toBigDecimal(), 0, 0)
                feeViewItemLiveData.postValue(FeeViewItem(fee, gasLimit))
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

        cautionsLiveData.postValue(cautionViewItemFactory.cautionViewItems(warnings, errors))
    }

    private fun sync(state: DataState<GasPriceInfo>) {
        if (state is DataState.Success) {
            sliderViewItemLiveData.postValue(
                SliderViewItem(
                    initialWeiValue = state.data.gasPrice.max,
                    weiRange = gasPriceService.gasPriceRange
                        ?: gasPriceService.defaultGasPriceRange,
                    stepSize = EvmFeeModule.stepSize(
                        gasPriceService.recommendedGasPrice
                            ?: gasPriceService.defaultGasPriceRange.first
                    ),
                    scale = coinService.token.blockchainType.feePriceScale
                )
            )
        }
    }

}
