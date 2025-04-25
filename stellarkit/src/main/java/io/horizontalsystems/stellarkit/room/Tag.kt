package io.horizontalsystems.stellarkit.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.stellarkit.TagQuery

@Entity
data class Tag(
    val eventId: Long,
    val type: Type? = null,
    val platform: Platform? = null,
//    val jettonAddress: Address? = null,
//    val addresses: List<Address> = listOf(),
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
) {
    enum class Platform {
        Native, Asset;
    }

    enum class Type {
        Incoming,
        Outgoing,
        Swap,
        Unsupported;
    }

    fun conforms(tagQuery: TagQuery): Boolean {
        if (tagQuery.type != type) {
            return false
        }

        if (tagQuery.platform != platform) {
            return false
        }

//        if (tagQuery.jettonAddress != jettonAddress) {
//            return false
//        }

//        if (!addresses.contains(tagQuery.address)) {
//            return false
//        }

        return true
    }
}
