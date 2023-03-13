package io.horizontalsystems.marketkit.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Blockchain(
    val type: BlockchainType,
    val name: String,
    val eip3091url: String?
) : Parcelable {

    val uid: String
        get() = type.uid

    override fun equals(other: Any?): Boolean =
        other is Blockchain && other.type == type

    override fun hashCode(): Int =
        Objects.hash(type, name)

}
