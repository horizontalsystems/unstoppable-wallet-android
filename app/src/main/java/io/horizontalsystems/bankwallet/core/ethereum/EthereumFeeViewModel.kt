package io.horizontalsystems.bankwallet.core.ethereum

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap.DataState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class EthereumFeeViewModel(
        private val transactionService: EthereumTransactionService,
        private val coinService: EthereumCoinService
) : ViewModel() {

    private val feeStatusSubject: BehaviorSubject<String> = BehaviorSubject.createDefault("")
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

    private fun sync(transactionStatus: DataState<EthereumTransactionService.Transaction>) {
        feeStatusSubject.onNext(feeStatus(transactionStatus))
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