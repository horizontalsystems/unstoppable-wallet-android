package io.horizontalsystems.stellarkit

import io.horizontalsystems.stellarkit.room.Tag

data class TagQuery(
    val type: Tag.Type?,
    val platform: Tag.Platform?,
//    val jettonAddress: Address?,
//    val address: Address?,
) {
    val isEmpty: Boolean
        get() = type == null && platform == null// && jettonAddress == null && address == null
}
