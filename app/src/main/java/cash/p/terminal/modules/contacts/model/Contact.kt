package cash.p.terminal.modules.contacts.model

import io.horizontalsystems.marketkit.models.Blockchain

data class Contact(
    val id: String,
    val name: String,
    val addresses: List<ContactAddress>
)

data class ContactAddress(
    val blockchain: Blockchain,
    val address: String
)
