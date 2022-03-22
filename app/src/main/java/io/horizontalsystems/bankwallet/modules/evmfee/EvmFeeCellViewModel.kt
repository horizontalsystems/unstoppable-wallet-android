package io.horizontalsystems.bankwallet.modules.evmfee

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.reactivex.disposables.CompositeDisposable

class EvmFeeCellViewModel(
    val feeService: IEvmFeeService,
    val gasPriceService: IEvmGasPriceService,
    val coinService: EvmCoinService
) : ViewModel() {

    private val disposable = CompositeDisposable()

    val feeLiveData = MutableLiveData("")
    val viewStateLiveData = MutableLiveData<ViewState>()
    val loadingLiveData = MutableLiveData<Boolean>()

    var highlightEditButton = false
        private set

    init {
        syncTransactionStatus(feeService.transactionStatus)
        feeService.transactionStatusObservable
            .subscribe { syncTransactionStatus(it) }
            .let { disposable.add(it) }
    }

    override fun onCleared() {
        disposable.clear()
    }

    private fun syncTransactionStatus(transactionStatus: DataState<Transaction>) {
        var hasError = false

        when (transactionStatus) {
            DataState.Loading -> {
                loadingLiveData.postValue(true)
            }
            is DataState.Error -> {
                hasError = true
                loadingLiveData.postValue(false)
                viewStateLiveData.postValue(ViewState.Error(transactionStatus.error))
                feeLiveData.postValue(Translator.getString(R.string.NotAvailable))
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data
                loadingLiveData.postValue(false)

                if (transaction.errors.isNotEmpty()) {
                    hasError = true
                    viewStateLiveData.postValue(ViewState.Error(transaction.errors.first()))
                } else {
                    viewStateLiveData.postValue(ViewState.Success)
                }

                val fee = coinService.amountData(transactionStatus.data.gasData.fee).getFormatted()
                feeLiveData.postValue(fee)
            }
        }

        highlightEditButton = hasError || !gasPriceService.isRecommendedGasPriceSelected
    }

}
