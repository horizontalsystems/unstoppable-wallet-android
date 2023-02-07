package cash.p.terminal.modules.evmfee.eip1559

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
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.disposables.CompositeDisposable

class Eip1559FeeSettingsViewModel(
    private val gasPriceService: Eip1559GasPriceService,
    feeService: IEvmFeeService,
    private val coinService: EvmCoinService,
    private val cautionViewItemFactory: CautionViewItemFactory
) : ViewModel() {

    private val disposable = CompositeDisposable()

    val baseFeeSliderViewItemLiveData = MutableLiveData<SliderViewItem>()
    val priorityFeeSliderViewItemLiveData = MutableLiveData<SliderViewItem>()
    val currentBaseFeeLiveData = MutableLiveData<String>()
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

    fun onSelectGasPrice(baseFee: Long, maxPriorityFee: Long) {
        gasPriceService.setGasPrice(baseFee, maxPriorityFee)
    }

    fun onClickReset() {
        gasPriceService.setRecommended()
    }

    override fun onCleared() {
        disposable.clear()
    }

    private fun sync(state: DataState<GasPriceInfo>) {
        sync(gasPriceService.currentBaseFee)

        state.dataOrNull?.let { gasPriceInfo ->
            if (gasPriceInfo.gasPrice is GasPrice.Eip1559) {
                baseFeeSliderViewItemLiveData.postValue(
                    SliderViewItem(
                        initialWeiValue = gasPriceInfo.gasPrice.maxFeePerGas - gasPriceInfo.gasPrice.maxPriorityFeePerGas,
                        weiRange = gasPriceService.baseFeeRange
                            ?: gasPriceService.defaultBaseFeeRange,
                        stepSize = EvmFeeModule.stepSize(
                            gasPriceService.currentBaseFee
                                ?: gasPriceService.defaultBaseFeeRange.first
                        ),
                        scale = coinService.token.blockchainType.feePriceScale
                    )
                )
                priorityFeeSliderViewItemLiveData.postValue(
                    SliderViewItem(
                        initialWeiValue = gasPriceInfo.gasPrice.maxPriorityFeePerGas,
                        weiRange = gasPriceService.priorityFeeRange
                            ?: gasPriceService.defaultPriorityFeeRange,
                        stepSize = EvmFeeModule.stepSize(
                            gasPriceService.currentPriorityFee
                                ?: gasPriceService.defaultPriorityFeeRange.first
                        ),
                        scale = coinService.token.blockchainType.feePriceScale
                    )
                )
            }
        }
    }

    private fun sync(currentBaseFee: Long?) {
        if (currentBaseFee != null) {
            currentBaseFeeLiveData.postValue(EvmFeeModule.scaledString(currentBaseFee, coinService.token.blockchainType.feePriceScale))
        } else {
            currentBaseFeeLiveData.postValue(Translator.getString(R.string.NotAvailable))
        }
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

}
