package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.TypeConverter
import java.math.BigDecimal

class DatabaseConverters {

    @TypeConverter
    fun fromString(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) } ?: null
    }

    @TypeConverter
    fun toString(bigDecimal: BigDecimal?): String? {
        return bigDecimal?.let {
            it.toPlainString()
        }
    }
}