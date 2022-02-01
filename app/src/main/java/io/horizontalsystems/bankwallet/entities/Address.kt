package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Address(val hex: String, val domain: String? = null) : Parcelable {
    val title: String
        get() = domain ?: hex
}
