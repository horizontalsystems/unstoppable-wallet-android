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

    private fun syncTransactionStatus(transactionStatus: DataState<Transaction>) {
        val feeStatusViewItem: FeeStatusViewItem
        val warnings = mutableListOf<Warning>()
        val errors = mutableListOf<Throwable>()

        when (transactionStatus) {
            DataState.Loading -> {
                val loading = Translator.getString(R.string.Alert_Loading)
                feeStatusViewItem = FeeStatusViewItem(loading, loading)
            }
            is DataState.Error -> {
                val notAvailable = Translator.getString(R.string.NotAvailable)
                feeStatusViewItem = FeeStatusViewItem(notAvailable, notAvailable)

                errors.add(transactionStatus.error)
            }
            is DataState.Success -> {
                val fee = coinService.amountData(transactionStatus.data.gasData.fee).getFormatted()
                val gasLimit = App.numberFormatter.format(transactionStatus.data.gasData.gasLimit.toBigDecimal(), 0, 0)

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
                    initialValue = gwei(state.data.gasPrice.max),
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
