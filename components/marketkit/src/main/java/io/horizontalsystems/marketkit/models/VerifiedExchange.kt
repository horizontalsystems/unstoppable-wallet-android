package io.horizontalsystems.marketkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class VerifiedExchange(
    @PrimaryKey
    val uid: String
)
