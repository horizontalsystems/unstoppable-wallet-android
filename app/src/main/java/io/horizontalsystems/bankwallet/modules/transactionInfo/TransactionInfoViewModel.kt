package io.horizontalsystems.bankwallet.modules.transactionInfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.NftMetadataManager
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.core.storage.OcpPaymentDao
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.transactions.NftMetadataService
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = TransactionInfoViewModel.Factory::class)
class TransactionInfoViewModel @AssistedInject constructor(
    @Assisted transactionRecord: TransactionRecord,
    transactionAdapterManager: TransactionAdapterManager,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
    nftMetadataManager: NftMetadataManager,
    balanceHiddenManager: BalanceHiddenManager,
    ocpPaymentDao: OcpPaymentDao,
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(transactionRecord: TransactionRecord): TransactionInfoViewModel
    }

    private val transactionSource = transactionRecord.source
    private val adapter = transactionAdapterManager.getAdapter(transactionSource)!!
    private val service = TransactionInfoService(
        transactionRecord,
        adapter,
        marketKit,
        currencyManager,
        NftMetadataService(nftMetadataManager),
        ocpPaymentDao,
        balanceHiddenManager.balanceHidden,
    )
    private val factory = TransactionInfoViewItemFactory(
        transactionSource.blockchain.type.resendable,
        transactionSource.blockchain.type
    )

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
