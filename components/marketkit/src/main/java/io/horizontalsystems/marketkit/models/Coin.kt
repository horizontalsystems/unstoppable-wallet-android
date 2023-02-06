package io.horizontalsystems.marketkit.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    indices = [
        Index(value = arrayOf("uid"))
    ]
)
data class Coin(
    @PrimaryKey
    val uid: String,
    val name: String,
    val code: String,
    val marketCapRank: Int? = null,
    val coinGeckoId: String? = null
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        return other is Coin && other.uid == uid
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun toString(): String {
        return "Coin [uid: $uid; name: $name; code: $code; marketCapRank: $marketCapRank; coinGeckoId: $coinGeckoId]"
    }
}
