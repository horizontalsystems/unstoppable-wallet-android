package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["url"])
data class ZanoNodeRecord(
    val url: String,
)
