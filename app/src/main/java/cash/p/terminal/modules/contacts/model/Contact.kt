package cash.p.terminal.modules.contacts.model

import android.os.Parcelable
import com.google.common.base.Objects
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
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        return other is ContactAddress && other.blockchain == blockchain && other.address == address
    }

    override fun hashCode(): Int {
        return Objects.hashCode(blockchain, address)
    }
}