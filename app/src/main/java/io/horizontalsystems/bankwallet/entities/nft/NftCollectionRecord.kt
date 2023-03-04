package io.horizontalsystems.bankwallet.entities.nft

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import io.horizontalsystems.bankwallet.core.storage.AccountRecord
import io.horizontalsystems.marketkit.models.BlockchainType

@Entity(
    primaryKeys = ["blockchainType", "accountId", "uid"],
    foreignKeys = [ForeignKey(
        entity = AccountRecord::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )
    ]
)
data class NftCollectionRecord(
    val blockchainType: BlockchainType,
    val accountId: String,
    val uid: String,
    val name: String,
    val imageUrl: String?,

    @Embedded(prefix = "averagePrice7d_")
    val averagePrice7d: NftPriceRecord?,

    @Embedded(prefix = "averagePrice30d_")
    val averagePrice30d: NftPriceRecord?
)
