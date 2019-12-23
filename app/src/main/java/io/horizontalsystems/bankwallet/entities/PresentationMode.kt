package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class PresentationMode(val value: String) : Parcelable {
    Initial("Initial"),
    InApp("InApp")
}