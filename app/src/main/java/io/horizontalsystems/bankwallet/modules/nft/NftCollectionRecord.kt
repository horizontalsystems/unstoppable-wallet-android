package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.Embedded
import androidx.room.Entity

@Entity(primaryKeys = ["accountId", "uid"])
data class NftCollectionRecord(
    val accountId: String,
    val uid: String,
    val name: String,
    val imageUrl: String?,

    @Embedded(prefix = "averagePrice7d_")
    val averagePrice7d: NftAssetPrice?,

    @Embedded(prefix = "averagePrice30d_")
    val averagePrice30d: NftAssetPrice?,

    @Embedded(prefix = "floorPrice_")
    val floorPrice: NftAssetPrice?,
)
