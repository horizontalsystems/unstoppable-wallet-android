package io.horizontalsystems.core.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Currency(val code: String, val symbol: String, val decimal: Int) : Parcelable
