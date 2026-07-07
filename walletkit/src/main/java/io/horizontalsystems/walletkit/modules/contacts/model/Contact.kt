package io.horizontalsystems.walletkit.modules.contacts.model

import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.serialization.Serializable
import java.util.Objects

@Serializable
data class Contact(
    val uid: String,
    val name: String,
    val addresses: List<ContactAddress>
)

@Serializable
data class ContactAddress(
    val blockchain: Blockchain,
    val address: String
) {
    override fun equals(other: Any?): Boolean {
        return other is ContactAddress && other.blockchain == blockchain && other.address.equals(address, ignoreCase = true)
    }

    override fun hashCode(): Int {
        return Objects.hash(blockchain, address.lowercase())
    }
}

data class ContactNameAddress(
    val name: String,
    val contactAddress: ContactAddress
)