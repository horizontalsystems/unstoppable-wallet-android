package io.horizontalsystems.bankwallet.modules.transactionInfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import kotlinx.coroutines.launch

class TransactionInfoViewModel(
    private val service: TransactionInfoService,
    private val factory: TransactionInfoViewItemFactory,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    val source: TransactionSource by service::source

    var viewItems by mutableStateOf<List<List<TransactionInfoViewItem>>>(listOf())
        private set

    init {
        viewModelScope.launch {
            contactsRepository.contactsFlow.collect {
                viewItems = factory.getViewItemSections(service.transactionInfoItem)
            }
        }
        viewModelScope.launch {
            service.transactionInfoItemFlow.collect { transactionInfoItem ->
                viewItems = factory.getViewItemSections(transactionInfoItem)
            }
        }

        viewModelScope.launch {
            service.start()
        }
    }

    fun getRawTransaction(): String? = service.getRawTransaction()
}
