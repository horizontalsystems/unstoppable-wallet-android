package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.Entity

@Entity(primaryKeys = ["accountId", "slug"])
data class NftCollection(
    val accountId: String,
    val slug: String,
    val name: String,
    val imageUrl: String,
)
