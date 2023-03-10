package cash.p.terminal.modules.contacts.model

import android.os.Parcelable
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact(
    val uid: String,
    val name: String,
    val addresses: List<ContactAddress>,
    val modifiedTimestamp: Long
) : Parcelable

@Parcelize
data class ContactAddress(
    val blockchain: Blockchain,
    val address: String
) : Parcelable
