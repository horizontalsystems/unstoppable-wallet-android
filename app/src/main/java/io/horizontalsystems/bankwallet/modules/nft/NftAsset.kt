package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.Entity

@Entity(primaryKeys = ["accountId", "tokenId"])
data class NftAsset(
    val accountId: String,
    val collectionSlug: String,
    val tokenId: String,
    val name: String,
    val imageUrl: String,
    val imagePreviewUrl: String
)
