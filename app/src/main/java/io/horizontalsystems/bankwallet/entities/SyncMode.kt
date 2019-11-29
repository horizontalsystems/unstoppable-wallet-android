package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class SyncMode(val value: String) : Parcelable {
    Fast("Fast"),
    Slow("Slow"),
    New("New");
}
