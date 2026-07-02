package io.horizontalsystems.core.entities

import androidx.room.Entity

@Entity(primaryKeys = ["url"])
data class ZanoNodeRecord(
    val url: String,
)
