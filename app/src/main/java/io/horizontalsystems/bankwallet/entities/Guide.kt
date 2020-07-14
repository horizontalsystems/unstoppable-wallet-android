package io.horizontalsystems.bankwallet.entities

import java.util.*

data class Guide(
        val title: String,
        val updatedAt: Date,
        val imageUrl: String?,
        val fileUrl: String
)

data class GuideCategory(val title: String) {
    var guides = listOf<Guide>()
}