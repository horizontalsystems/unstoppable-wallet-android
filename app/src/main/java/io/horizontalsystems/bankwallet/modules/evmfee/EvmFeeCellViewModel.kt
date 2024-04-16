package io.horizontalsystems.bankwallet.modules.evmfee

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.fee.FeeItem
import kotlinx.coroutines.launch

class EvmFeeCellViewModel(
    val feeService: IEvmFeeService,
    val gasPriceService: IEvmGasPriceService,
    val coinService: EvmCoinService
) : ViewModel() {

    val feeLiveData = MutableLiveData<FeeItem?>()
    val viewStateLiveData = MutableLiveData<ViewState>()

    init {
        viewModelScope.launch {
            feeService.transactionStatusFlow.collect {
                syncTransactionStatus(it)
            }
        }
    }

    private fun syncTransactionStatus(transactionStatus: DataState<Transaction>) {
        when (transactionStatus) {
            DataState.Loading -> {}
            is DataState.Error -> {
                viewStateLiveData.postValue(ViewState.Error(transactionStatus.error))
                feeLiveData.postValue(null)
            }
            is DataState.Success -> {
                val transaction = transactionStatus.data

                if (transaction.errors.isNotEmpty()) {
                    viewStateLiveData.postValue(ViewState.Error(transaction.errors.first()))
                } else {
                    viewStateLiveData.postValue(ViewState.Success)
                }

                val feeAmountData = coinService.amountData(transactionStatus.data.gasData.estimatedFee, transactionStatus.data.gasData.isSurcharged)
                val feeViewItem = FeeItem(
                    primary = feeAmountData.primary.getFormattedPlain(),
                    secondary = feeAmountData.secondary?.getFormattedPlain()
                )
                feeLiveData.postValue(feeViewItem)
            }
        }
    }
}
