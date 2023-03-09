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
        "3" to Contact("3", "John", listOf(
            ContactAddress(Blockchain(BlockchainType.Ethereum, "Ethereum", null), "0xae090025857b9c7b24387741f120538e928a3a12")
        )),
    )

    val contacts: List<Contact>
        get() = contactsMap.map { it.value }.toList()

    private val _contactsFlow = MutableStateFlow(contacts)
    val contactsFlow: StateFlow<List<Contact>> = _contactsFlow

    fun getContactsFiltered(
        blockchainType: BlockchainType,
        nameQuery: String? = null,
        addressQuery: String? = null,
    ): List<Contact> {
        val criteria = mutableListOf<(Contact) -> Boolean>()

        nameQuery?.let {
            criteria.add {
                it.name.contains(nameQuery, true)
            }
        }

        criteria.add {
            it.addresses.isNotEmpty()
        }

        if (addressQuery != null) {
            criteria.add {
                it.addresses.any {
                    it.blockchain.type == blockchainType && it.address.equals(addressQuery, true)
                }
            }
        } else {
            criteria.add {
                it.addresses.any { it.blockchain.type == blockchainType }
            }
        }

        return contacts.filter { contact ->
            criteria.all {
                it.invoke(contact)
            }
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
