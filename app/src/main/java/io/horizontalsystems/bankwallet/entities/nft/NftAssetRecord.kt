package io.horizontalsystems.bankwallet.entities.nft

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import io.horizontalsystems.bankwallet.core.storage.AccountRecord
import io.horizontalsystems.marketkit.models.BlockchainType

@Entity(
    primaryKeys = ["blockchainType", "accountId", "nftUid"],
    foreignKeys = [ForeignKey(
        entity = AccountRecord::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )
    ]
)
data class NftAssetRecord(
    val blockchainType: BlockchainType,
    val accountId: String,
    val nftUid: NftUid,
    val collectionUid: String,
    val name: String?,
    val imagePreviewUrl: String?,
    val onSale: Boolean,

    @Embedded(prefix = "lastSale_")
    val lastSale: NftPriceRecord?
)
