package io.horizontalsystems.bankwallet.entities.nft

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class NftAssetBriefMetadataRecord(
    @PrimaryKey
    val nftUid: NftUid,
    val providerCollectionUid: String,
    val name: String?,
    val imageUrl: String?,
    val previewImageUrl: String?
)