package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Guide(
        val title: String,
        val updatedAt: Date,
        val imageUrl: String?,
        val fileUrl: String
) : Parcelable

data class GuideCategory(val title: String) {
    var guides = listOf<Guide>()
}