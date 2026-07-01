package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class SyncMode(val value: String) : Parcelable {
    Fast("Fast"),
    Slow("Slow"),
    New("New");

    val title: String
        get() = when (this) {
            New -> "API"
            Fast -> "API"
            Slow -> "Blockchain"
        }
}
