package cash.p.terminal.modules.transactionInfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.wallet.transaction.TransactionSource
import kotlinx.coroutines.launch

class TransactionInfoViewModel(
    private val service: TransactionInfoService,
    private val factory: TransactionInfoViewItemFactory,
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    val source: TransactionSource by service::source
    val transactionRecord by service::transactionRecord

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
