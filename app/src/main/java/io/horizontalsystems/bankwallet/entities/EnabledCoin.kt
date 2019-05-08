package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EnabledCoin(
        @PrimaryKey
        val coinCode: String,
        var order: Int? = null)
