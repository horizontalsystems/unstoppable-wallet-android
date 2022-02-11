package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.Embedded
import androidx.room.Entity
import java.math.BigDecimal

@Entity(primaryKeys = ["accountId", "tokenId"])
data class NftAsset(
    val accountId: String,
    val collectionSlug: String,
    val tokenId: String,
    val name: String,
    val imageUrl: String,
    val imagePreviewUrl: String,

    @Embedded
    val lastSale: NftAssetLastSale?
)

data class NftAssetLastSale(
    val coinTypeId: String,
    val totalPrice: BigDecimal
)
