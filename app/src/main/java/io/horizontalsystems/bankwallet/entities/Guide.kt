package io.horizontalsystems.bankwallet.entities

import java.util.*

data class Guide(
        val title: String,
        val updatedAt: Date,
        val imageUrl: String?,
        val fileUrl: String
)

data class GuideCategory(
        val id: String,
        val category: String,
        val guides: List<Guide>)

data class GuideCategoryMultiLang(
        val id: String,
        val category: Map<String,String>){
        var guides = listOf<Map<String, Guide>>()
}
