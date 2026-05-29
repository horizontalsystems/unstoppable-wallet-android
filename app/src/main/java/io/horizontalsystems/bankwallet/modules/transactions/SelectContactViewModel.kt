package io.horizontalsystems.bankwallet.modules.transactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.BlockchainType

@HiltViewModel(assistedFactory = SelectContactViewModel.Factory::class)
class SelectContactViewModel @AssistedInject constructor(
    private val contactsRepository: ContactsRepository,
    @Assisted private val selected: Contact?,
    @Assisted private val blockchainType: BlockchainType?,
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

    @AssistedFactory
    interface Factory {
        fun create(selected: Contact?, blockchainType: BlockchainType?): SelectContactViewModel
    }

    companion object {
        val supportedBlockchainTypes =
            EvmBlockchainManager.blockchainTypes +
                    BlockchainType.Zcash +
                    BlockchainType.Tron +
                    BlockchainType.Ton +
                    BlockchainType.Stellar
    }
}

data class SelectContactUiState(
    val items: List<Contact?>,
    val selected: Contact?,
)
