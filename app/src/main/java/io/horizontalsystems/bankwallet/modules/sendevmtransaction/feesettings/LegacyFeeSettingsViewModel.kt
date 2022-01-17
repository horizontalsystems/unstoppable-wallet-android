package io.horizontalsystems.bankwallet.modules.sendevmtransaction.feesettings

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.*
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.reactivex.disposables.CompositeDisposable

class LegacyFeeSettingsViewModel(
    private val gasPriceService: LegacyGasPriceService,
    private val feeService: IEvmTransactionFeeService,
    private val coinService: EvmCoinService,
    private val cautionViewItemFactory: CautionViewItemFactory
) : ViewModel() {

    private val disposable = CompositeDisposable()

    private val gasPriceUnit: String = "gwei"
    val sliderViewItemLiveData = MutableLiveData<SendFeeSliderViewItem>()
    val feeStatusLiveData = MutableLiveData<FeeStatusViewItem>()
    val cautionsLiveData = MutableLiveData<List<CautionViewItem>>()

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

    private fun syncTransactionStatus(transactionStatus: DataState<EvmTransactionFeeService.Transaction>) {
        val feeStatusViewItem: FeeStatusViewItem
        val warnings = mutableListOf<Warning>()
        val errors = mutableListOf<Throwable>()

        when (transactionStatus) {
            DataState.Loading -> {
                Log.e("AAA", "feeStatus: Loading")
                val loading = Translator.getString(R.string.Alert_Loading)
                feeStatusViewItem = FeeStatusViewItem(loading, loading)
            }
            is DataState.Error -> {
                Log.e("AAA", "feeStatus: Error ${transactionStatus.error.javaClass.simpleName}")
                val notAvailable = Translator.getString(R.string.NotAvailable)
                feeStatusViewItem = FeeStatusViewItem(notAvailable, notAvailable)

                errors.add(transactionStatus.error)
            }
            is DataState.Success -> {
                val fee = coinService.amountData(transactionStatus.data.gasData.fee).getFormatted()
                val gasLimit = App.numberFormatter.format(transactionStatus.data.gasData.gasLimit.toBigDecimal(), 0, 0)
                Log.e("AAA", "feeStatus: Success fee= $fee, gasLimit= $gasLimit")

                feeStatusViewItem = FeeStatusViewItem(fee, gasLimit)

                warnings.addAll(transactionStatus.data.warnings)
                errors.addAll(transactionStatus.data.errors)
            }
        }

        cautionsLiveData.postValue(cautionViewItemFactory.cautionViewItems(warnings, errors))
        feeStatusLiveData.postValue(feeStatusViewItem)
    }

    private fun sync(state: DataState<GasPriceInfo>) {
        if (state is DataState.Success) {
            sliderViewItemLiveData.postValue(
                SendFeeSliderViewItem(
                    initialValue = gwei(state.data.gasPrice.value),
                    range = gwei(gasPriceService.gasPriceRange),
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
