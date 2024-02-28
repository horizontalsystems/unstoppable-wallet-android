package io.horizontalsystems.bankwallet.modules.transactions

import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.BlockchainType

class SelectContactViewModel(
    private val contactsRepository: ContactsRepository,
    private val selected: Contact?,
    private val blockchainType: BlockchainType?,
) : ViewModelUiState<SelectContactUiState>() {

    private var items: List<Contact?>

    init {
        val contacts = fetchContacts()

        items = when {
            contacts.isNotEmpty() -> listOf(null) + contacts
            else -> contacts
        }
        emitState()
    }

    private fun fetchContacts(): List<Contact> {
        if (blockchainType != null && !supportedBlockchainTypes.contains(blockchainType)) {
            return listOf()
        }

        return contactsRepository.getContactsFiltered(blockchainType).filter { contact ->
            contact.addresses.any {
                supportedBlockchainTypes.contains(it.blockchain.type)
            }
        }
    }

    override fun createState() = SelectContactUiState(
        items = items,
        selected = selected
    )

    companion object {
        val supportedBlockchainTypes = EvmBlockchainManager.blockchainTypes + BlockchainType.Tron

        fun init(selected: Contact?, blockchainType: BlockchainType?): CreationExtras.() -> SelectContactViewModel = {
            SelectContactViewModel(App.contactsRepository, selected, blockchainType)
        }
    }
}

data class SelectContactUiState(
    val items: List<Contact?>,
    val selected: Contact?,
)
