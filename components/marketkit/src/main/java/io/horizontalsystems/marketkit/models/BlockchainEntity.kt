package io.horizontalsystems.marketkit.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = arrayOf("uid"))
    ]
)
data class BlockchainEntity(
    @PrimaryKey
    val uid: String,
    val name: String,
    val eip3091url: String?
) {

    val blockchain: Blockchain
        get() = Blockchain(BlockchainType.fromUid(uid), name, eip3091url)

}
