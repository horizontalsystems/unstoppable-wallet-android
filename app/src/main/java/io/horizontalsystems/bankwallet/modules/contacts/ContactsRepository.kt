package io.horizontalsystems.bankwallet.modules.contacts

import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ContactsRepository {

    private val contactsMap = mutableMapOf(
        "1" to Contact(
            "1",
            "Alice",
            listOf(
                ContactAddress(Blockchain(BlockchainType.Solana, "Solana", null), "EbBULL18qZupyQXUQ1BiPeaY2ac2dVsaeCmNGfaNonWh"),
                ContactAddress(Blockchain(BlockchainType.Bitcoin, "Bitcoin", null), "1DHtWf6Nhd3Mxq4bdtMNtyogVgYAU9SrmC"),
                ContactAddress(Blockchain(BlockchainType.Ethereum, "Ethereum", null), "0xd2090025857b9c7b24387741f120538e928a3a59"),
            )
        ),
        "2" to Contact("2", "Bob", listOf()),
        "3" to Contact("3", "John", listOf()),
    )

    val contacts: List<Contact>
        get() = contactsMap.map { it.value }.toList()

    private val _contactsFlow = MutableStateFlow(contacts)
    val contactsFlow: StateFlow<List<Contact>> = _contactsFlow

    fun getContactsByBlockchainType(blockchainType: BlockchainType): List<Contact> {
        return contacts.filter {
            it.addresses.isNotEmpty() && it.addresses.any { it.blockchain.type == blockchainType }
        }
    }

    fun save(contact: Contact) {
        contactsMap[contact.id] = contact
        _contactsFlow.update { contacts }
    }

    fun get(id: String): Contact? {
        return contactsMap[id]
    }

    fun delete(id: String) {
        contactsMap.remove(id)
        _contactsFlow.update { contacts }
    }

}
