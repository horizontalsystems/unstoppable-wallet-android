package cash.p.terminal.modules.contacts.model

import android.os.Parcelable
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact(
    val id: String,
    val name: String,
    val addresses: List<ContactAddress>
): Parcelable

@Parcelize
data class ContactAddress(
    val blockchain: Blockchain,
    val address: String
) : Parcelable
