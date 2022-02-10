package io.horizontalsystems.bankwallet.modules.evmfee.eip1559

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.evmfee.*
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.math.RoundingMode

class Eip1559FeeSettingsViewModel(
    private val gasPriceService: Eip1559GasPriceService,
    private val feeService: IEvmFeeService,
    private val coinService: EvmCoinService,
    private val cautionViewItemFactory: CautionViewItemFactory
) : ViewModel() {

    private val disposable = CompositeDisposable()
    private val gasPriceUnit: String = "Gwei"

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
        gasPriceService.setGasPrice(wei(baseFee), wei(maxPriorityFee))
    }

    fun onClickReset() {
        gasPriceService.setRecommended()
    }

    private fun sync(state: DataState<GasPriceInfo>) {
        sync(gasPriceService.currentBaseFee)

        state.dataOrNull?.let { gasPriceInfo ->
            if (gasPriceInfo.gasPrice is GasPrice.Eip1559) {
                baseFeeSliderViewItemLiveData.postValue(
                    SliderViewItem(
                        initialValue = gwei(gasPriceInfo.gasPrice.maxFeePerGas - gasPriceInfo.gasPrice.maxPriorityFeePerGas),
                        range = gwei(gasPriceService.baseFeeRange ?: gasPriceService.defaultBaseFeeRange),
                        unit = gasPriceUnit
                    )
                )
                priorityFeeSliderViewItemLiveData.postValue(
                    SliderViewItem(
                        initialValue = gwei(gasPriceInfo.gasPrice.maxPriorityFeePerGas),
                        range = gwei(gasPriceService.priorityFeeRange ?: gasPriceService.defaultPriorityFeeRange),
                        unit = gasPriceUnit
                    )
                )
            }
        }
    }

    private fun sync(currentBaseFee: Long?) {
        if (currentBaseFee != null) {
            currentBaseFeeLiveData.postValue("${gwei(currentBaseFee)} $gasPriceUnit")
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


    private fun wei(gwei: Long): Long {
        return gwei * 1_000_000_000
    }

    private fun gwei(wei: Long): Long {
        return BigDecimal(wei).divide(BigDecimal(1_000_000_000), RoundingMode.CEILING).toLong()
    }

    private fun gwei(range: LongRange): LongRange {
        return LongRange(gwei(range.first), gwei(range.last))
    }
}
