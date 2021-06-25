package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Address(val hex: String, val domain: String? = null) : Parcelable {
    val title: String
        get() = domain ?: hex
}
