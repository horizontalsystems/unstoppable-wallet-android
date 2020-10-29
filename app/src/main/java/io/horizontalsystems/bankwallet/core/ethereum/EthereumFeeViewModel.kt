package io.horizontalsystems.bankwallet.core.ethereum

import android.util.Range
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

    sealed class Priority {
        abstract val description: String

        object Recommended : Priority() {
            override val description by lazy { App.instance.getString(R.string.Send_TxSpeed_Recommended) }
        }

        object Custom : Priority() {
            override val description by lazy { App.instance.getString(R.string.Send_TxSpeed_Custom) }
        }
    }

    val feeStatusLiveData = MutableLiveData<String>("")
    val priorityLiveData = MutableLiveData<String>("")
    val feeSliderLiveData = MutableLiveData<SendFeeSliderViewItem?>(null)

    private val customFeeUnit = "gwei"
    private val customFeeRange = Range(1, 400)
    private val disposable = CompositeDisposable()

    init {
        syncTransactionStatus(transactionService.transactionStatus)
        syncGasPriceType(transactionService.gasPriceType)

        transactionService.transactionStatusObservable
                .subscribe { transactionStatus ->
                    syncTransactionStatus(transactionStatus)
                }
                .let {
                    disposable.add(it)
                }

        transactionService.gasPriceTypeObservable
                .subscribe { gasPriceType ->
                    syncGasPriceType(gasPriceType)
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun syncTransactionStatus(transactionStatus: DataState<EthereumTransactionService.Transaction>) {
        feeStatusLiveData.postValue(feeStatus(transactionStatus))
    }

    private fun syncGasPriceType(gasPriceType: EthereumTransactionService.GasPriceType) {
        priorityLiveData.postValue(getPriority(gasPriceType).description)

        when (gasPriceType) {
            EthereumTransactionService.GasPriceType.Recommended -> {
                feeSliderLiveData.postValue(null)
            }
            is EthereumTransactionService.GasPriceType.Custom -> {
                if (feeSliderLiveData.value == null) {
                    val gasPrice = gasPriceType.gasPrice
                    feeSliderLiveData.postValue(SendFeeSliderViewItem(initialValue = gwei(wei = gasPrice), range = customFeeRange, unit = customFeeUnit))
                }
            }
        }
    }

    private fun gwei(wei: Long): Long {
        return wei / 1_000_000_000
    }

    private fun getPriority(gasPriceType: EthereumTransactionService.GasPriceType): Priority {
        return when (gasPriceType) {
            EthereumTransactionService.GasPriceType.Recommended -> Priority.Recommended
            is EthereumTransactionService.GasPriceType.Custom -> Priority.Custom
        }
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

data class SendFeeSliderViewItem(val initialValue: Long, val range: Range<Int>, val unit: String)
