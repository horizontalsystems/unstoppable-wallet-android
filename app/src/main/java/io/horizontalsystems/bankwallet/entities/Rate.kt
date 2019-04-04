package io.horizontalsystems.bankwallet.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.bankwallet.core.storage.DatabaseConverters
import java.math.BigDecimal
import java.util.*

@Entity(primaryKeys = ["coinCode", "currencyCode", "timestamp", "isLatest"])
@TypeConverters(DatabaseConverters::class)
data class Rate(var coinCode: String,
                var currencyCode: String,
                var value: BigDecimal,
                var timestamp: Long,
                var isLatest: Boolean) {

    val expired: Boolean
        get() {
            val diff = Date().time - timestamp
            return diff > 60 * 10 * 1000
        }
}
