package io.horizontalsystems.bankwallet.core.ethereum

import android.util.Range
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class EthereumFeeViewModel(
        val transactionService: EthereumTransactionService,
        private val coinService: CoinService
) : ViewModel(), ISendFeeViewModel, ISendFeePriorityViewModel {

    enum class Priority {
        Recommended {
            override val description by lazy { App.instance.getString(R.string.Send_TxSpeed_Recommended) }
        },
        Custom {
            override val description by lazy { App.instance.getString(R.string.Send_TxSpeed_Custom) }
        };

        abstract val description: String
    }

    override val estimatedFeeLiveData = MutableLiveData<String>("")
    override val feeLiveData = MutableLiveData<String>("")

    override val priorityLiveData = MutableLiveData<String>("")
    override val openSelectPriorityLiveEvent = SingleLiveEvent<List<SendPriorityViewItem>>()
    override val feeSliderLiveData = MutableLiveData<SendFeeSliderViewItem?>(null)

    private val customFeeUnit = "gwei"
    private val customFeeRange = Range(1L, 400L)
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

    override fun openSelectPriority() {
        val currentPriority = getPriority(transactionService.gasPriceType)

        val viewItems = Priority.values().map {
            SendPriorityViewItem(it.description, currentPriority == it)
        }

        openSelectPriorityLiveEvent.postValue(viewItems)
    }

    override fun selectPriority(index: Int) {
        val selectedPriority = Priority.values().get(index)
        val currentPriority = getPriority(transactionService.gasPriceType)

        if (selectedPriority == currentPriority) return

        transactionService.gasPriceType = when (selectedPriority) {
            Priority.Recommended -> {
                EthereumTransactionService.GasPriceType.Recommended
            }
            Priority.Custom -> {
                val transaction = transactionService.transactionStatus.dataOrNull
                val gasPrice = transaction?.gasData?.gasPrice ?: wei(customFeeRange.lower)

                EthereumTransactionService.GasPriceType.Custom(gasPrice)
            }
        }
    }

    override fun changeCustomPriority(value: Long) {
        transactionService.gasPriceType = EthereumTransactionService.GasPriceType.Custom(wei(value))
    }

    private fun syncTransactionStatus(transactionStatus: DataState<EthereumTransactionService.Transaction>) {
        estimatedFeeLiveData.postValue(estimatedFeeStatus(transactionStatus))
        feeLiveData.postValue(feeStatus(transactionStatus))
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

    private fun wei(gwei: Long): Long {
        return gwei * 1_000_000_000
    }

    private fun getPriority(gasPriceType: EthereumTransactionService.GasPriceType): Priority {
        return when (gasPriceType) {
            EthereumTransactionService.GasPriceType.Recommended -> Priority.Recommended
            is EthereumTransactionService.GasPriceType.Custom -> Priority.Custom
        }
    }

    private fun estimatedFeeStatus(transactionStatus: DataState<EthereumTransactionService.Transaction>): String {
        return when (transactionStatus) {
            DataState.Loading -> {
                App.instance.getString(R.string.Alert_Loading)
            }
            is DataState.Error -> {
                App.instance.getString(R.string.NotAvailable)
            }
            is DataState.Success -> {
                coinService.amountData(transactionStatus.data.gasData.estimatedFee).getFormatted()
            }
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

data class SendFeeSliderViewItem(val initialValue: Long, val range: Range<Long>, val unit: String)
data class SendPriorityViewItem(val title: String, val selected: Boolean)

interface ISendFeeViewModel {
    val estimatedFeeLiveData: MutableLiveData<String>
    val feeLiveData: LiveData<String>
}

interface ISendFeePriorityViewModel {
    val priorityLiveData: LiveData<String>
    val openSelectPriorityLiveEvent: SingleLiveEvent<List<SendPriorityViewItem>>
    val feeSliderLiveData: LiveData<SendFeeSliderViewItem?>

    fun openSelectPriority()
    fun selectPriority(index: Int)
    fun changeCustomPriority(value: Long)
}
