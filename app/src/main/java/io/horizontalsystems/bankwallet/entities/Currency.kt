package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Currency(
    val code: String,
    val symbol: String,
    val decimal: Int,
    val flag: Int
) : Parcelable
