package io.horizontalsystems.stellarkit

import io.horizontalsystems.stellarkit.room.Tag

data class TagQuery(
    val type: Tag.Type?,
    val assetId: String?,
    val accountId: String?,
) {
    val isEmpty = type == null && assetId == null && accountId == null
}
