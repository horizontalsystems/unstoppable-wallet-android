package io.horizontalsystems.marketkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Exchange(
    @PrimaryKey
    val id: String,
    val name: String,
    val imageUrl: String
)

data class ExchangeRaw(
    val id: String,
    val name: String,
    val image: String
)
