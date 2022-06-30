package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import io.horizontalsystems.bankwallet.core.storage.AccountRecord
import java.math.BigDecimal

@Entity(
    primaryKeys = ["accountId", "tokenId", "contract_address"],
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
    val accountId: String,
    val collectionUid: String,
    val tokenId: String,
    val name: String?,
    val imageUrl: String?,
    val imagePreviewUrl: String?,
    val description: String?,
    val onSale: Boolean,

    @Embedded
    val lastSale: NftAssetPrice?,

    @Embedded(prefix = "contract_")
    val contract: NftAssetContract,

    @Embedded
    val links: AssetLinks?,

    val attributes: List<NftAssetAttribute>
)

data class NftAssetPrice(
    val tokenQueryId: String,
    val value: BigDecimal
)

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
