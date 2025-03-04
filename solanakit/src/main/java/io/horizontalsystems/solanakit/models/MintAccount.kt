package io.horizontalsystems.solanakit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class MintAccount(
    @PrimaryKey
    val address: String,
    val decimals: Int,
    val supply: Long? = null,
    val isNft: Boolean = false,
    val name: String? = null,
    val symbol: String? = null,
    val uri: String? = null,
    val collectionAddress: String? = null
)
