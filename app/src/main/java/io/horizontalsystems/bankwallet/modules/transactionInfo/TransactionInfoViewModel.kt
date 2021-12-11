package io.horizontalsystems.bankwallet.modules.transactionInfo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.transactionInfo.adapters.TransactionInfoViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class TransactionInfoViewModel(
    private val service: TransactionInfoService,
    private val factory: TransactionInfoViewItemFactory,
    private val clearables: List<Clearable>
) : ViewModel() {
    val source: TransactionSource by service::source

    val showShareLiveEvent = SingleLiveEvent<String>()
    val copyRawTransactionLiveEvent = SingleLiveEvent<String>()
    val openTransactionOptionsModule = SingleLiveEvent<Pair<TransactionInfoOption.Type, String>>()

    val viewItemsLiveData = MutableLiveData<List<TransactionInfoViewItem?>>()

    private val disposables = CompositeDisposable()

    init {
        service.transactionInfoItemObservable
            .subscribeIO {
                updateViewItems(it)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun updateViewItems(transactionItem: TransactionInfoItem) {
        val viewItems = factory.getMiddleSectionItems(transactionItem.record, transactionItem.rates, transactionItem.lastBlockInfo, transactionItem.explorerData)
        viewItemsLiveData.postValue(viewItems)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

    fun onActionButtonClick(actionButton: TransactionInfoActionButton) {
        when (actionButton) {
            is TransactionInfoActionButton.ShareButton -> showShareLiveEvent.postValue(actionButton.value)
            is TransactionInfoActionButton.CopyButton -> copyRawTransactionLiveEvent.postValue(service.getRaw())
        }
    }

    fun onOptionButtonClick(optionType: TransactionInfoOption.Type) {
        openTransactionOptionsModule.postValue(Pair(optionType, service.transactionHash))
    }

}
