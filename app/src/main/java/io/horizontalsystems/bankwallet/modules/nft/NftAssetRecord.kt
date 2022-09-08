package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import io.horizontalsystems.bankwallet.core.storage.AccountRecord
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.NftPrice
import java.math.BigDecimal

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

data class NftPriceRecord(
    val tokenQueryId: String,
    val value: BigDecimal
) {
    constructor(nftPrice: NftPrice) : this(nftPrice.token.tokenQuery.id, nftPrice.value)
}

data class NftAssetContract(
    val address: String,
    val type: String
)

data class NftAssetAttribute(
    val type: String,
    val value: String,
    val count: Int
)

data class AssetLinks(
    val external_link: String?,
    val permalink: String,
)
