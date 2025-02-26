package io.horizontalsystems.solanakit.database.transaction

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.math.BigInteger

class RoomTypeConverters {
    @TypeConverter
    fun bigIntegerFromString(string: String?): BigInteger? {
        return string?.let { BigInteger(it) }
    }

    @TypeConverter
    fun bigIntegerToString(bigInteger: BigInteger?): String? {
        return bigInteger?.toString()
    }

    @TypeConverter
    fun bigDecimalFromString(string: String?): BigDecimal? {
        return string?.let { BigDecimal(it) }
    }

    @TypeConverter
    fun bigDecimalToString(bigDecimal: BigDecimal?): String? {
        return bigDecimal?.toString()
    }
}