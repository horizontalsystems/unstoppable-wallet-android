package io.horizontalsystems.bankwallet.modules.receive.usedaddress

import android.os.Parcelable
import io.horizontalsystems.bankwallet.core.UsedAddress
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsedAddressesParams(
    val coinName: String,
    val usedAddresses: List<UsedAddress>
): Parcelable
