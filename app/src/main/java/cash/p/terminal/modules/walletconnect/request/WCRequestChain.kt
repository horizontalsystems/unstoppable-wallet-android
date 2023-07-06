package cash.p.terminal.modules.walletconnect.request

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WCRequestChain(
    val name: String,
    val address: String
): Parcelable
