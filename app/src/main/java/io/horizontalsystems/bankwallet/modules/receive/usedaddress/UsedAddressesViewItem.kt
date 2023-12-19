package cash.p.terminal.modules.receive.usedaddress

import android.os.Parcelable
import cash.p.terminal.core.UsedAddress
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsedAddressesViewItem(
    val coinName: String,
    val usedAddresses: List<UsedAddress>
): Parcelable
