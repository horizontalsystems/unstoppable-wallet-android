package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Guide(
        val title: String,
        val date: Date,
        val imageUrl: String,
        val fileName: String
) : Parcelable
