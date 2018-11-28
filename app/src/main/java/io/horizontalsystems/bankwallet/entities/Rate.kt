package io.horizontalsystems.bankwallet.entities

import android.arch.persistence.room.Entity

@Entity(primaryKeys = ["coin", "currencyCode"])
data class Rate(var coin: String,
                var currencyCode: String,
                var value: Double,
                var timestamp: Long = 0)
