package io.horizontalsystems.bankwallet.core.ethereum

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap.DataState
import io.reactivex.disposables.CompositeDisposable

class EthereumFeeViewModel(
        private val transactionService: EthereumTransactionService,
        private val coinService: EthereumCoinService
) : ViewModel() {

    private val feeStatusLiveData = MutableLiveData<String>()
    private val disposable = CompositeDisposable()

    init {
        sync(transactionService.transactionStatus)

        transactionService.transactionStatusObservable
                .subscribe { transactionStatus ->
                    sync(transactionStatus)
                }
                .let {
                    disposable.add(it)
                }
    }

    fun setGasPriceType(gasPriceType: EthereumTransactionService.GasPriceType) {
        transactionService.gasPriceType = gasPriceType
    }

    private fun sync(transactionStatus: DataState<EthereumTransactionService.Transaction>) {
        feeStatusLiveData.postValue(feeStatus(transactionStatus))
    }

    private fun feeStatus(transactionStatus: DataState<EthereumTransactionService.Transaction>): String {
        return when (transactionStatus) {
            DataState.Loading -> {
                App.instance.getString(R.string.Alert_Loading)
            }
            is DataState.Error -> {
                App.instance.getString(R.string.NotAvailable)
            }
            is DataState.Success -> {
                coinService.amountData(transactionStatus.data.gasData.fee).getFormatted()
            }
        }
    }

}
