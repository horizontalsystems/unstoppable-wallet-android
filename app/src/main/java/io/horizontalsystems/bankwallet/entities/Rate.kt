package io.horizontalsystems.bankwallet.entities

import android.arch.persistence.room.Entity
import java.util.*

@Entity(primaryKeys = ["coinCode", "currencyCode", "timestamp", "isLatest"])
data class Rate(var coinCode: String,
                var currencyCode: String,
                var value: Double,
                var timestamp: Long,
                var isLatest: Boolean) {

    val expired: Boolean
        get() {
            val diff = (Date().time / 1000) - timestamp
            return diff > 60 * 10
        }
}
