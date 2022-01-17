package io.horizontalsystems.bankwallet.core.ethereum

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionFeeService.Transaction
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.DataState
import io.reactivex.disposables.CompositeDisposable

class EvmFeeCellViewModel(
    val feeService: IEvmTransactionFeeService,
    val coinService: EvmCoinService
) : ViewModel() {

    private val disposable = CompositeDisposable()

    val feeLiveData = MutableLiveData("")

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
        val feeStatus = when (transactionStatus) {
            DataState.Loading -> {
                Translator.getString(R.string.Alert_Loading)
            }
            is DataState.Error -> {
                Translator.getString(R.string.NotAvailable)
            }
            is DataState.Success -> {
                coinService.amountData(transactionStatus.data.gasData.fee).getFormatted()
            }
        }

        feeLiveData.postValue(feeStatus)
    }

}
