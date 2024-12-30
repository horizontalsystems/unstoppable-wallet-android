package cash.p.terminal.wallet.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsedAddress(
    val index: Int,
    val address: String,
    val explorerUrl: String
): Parcelable