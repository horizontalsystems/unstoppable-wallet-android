package io.horizontalsystems.bankwallet.modules.evmfee.legacy

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
import io.reactivex.disposables.CompositeDisposable

class LegacyFeeSettingsViewModel(
    private val gasPriceService: LegacyGasPriceService,
    private val feeService: IEvmFeeService,
    private val coinService: EvmCoinService,
    private val cautionViewItemFactory: CautionViewItemFactory
) : ViewModel() {

    private val disposable = CompositeDisposable()

    private val gasPriceUnit: String = "gwei"
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
        gasPriceService.setGasPrice(wei(gasPrice))
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
                    initialValue = gwei(state.data.gasPrice.max),
                    range = gwei(gasPriceService.gasPriceRange ?: gasPriceService.defaultGasPriceRange),
                    unit = gasPriceUnit
                )
            )
        }
    }

    private fun wei(gwei: Long): Long {
        return gwei * 1_000_000_000
    }

    private fun gwei(wei: Long): Long {
        return wei / 1_000_000_000
    }

    private fun gwei(range: LongRange): LongRange {
        return LongRange(gwei(range.first), gwei(range.last))
    }
}
