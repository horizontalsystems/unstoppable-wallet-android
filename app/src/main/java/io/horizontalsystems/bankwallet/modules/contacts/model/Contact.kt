package io.horizontalsystems.bankwallet.modules.contacts.model

import android.os.Parcelable
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.parcelize.Parcelize
import java.util.Objects

@Parcelize
data class Contact(
    val uid: String,
    val name: String,
    val addresses: List<ContactAddress>
) : Parcelable

@Parcelize
data class ContactAddress(
    val blockchain: Blockchain,
    val address: String
) : Parcelable {
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