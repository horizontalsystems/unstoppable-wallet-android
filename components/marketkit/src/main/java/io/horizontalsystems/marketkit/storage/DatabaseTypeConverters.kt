package io.horizontalsystems.marketkit.storage

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.marketkit.models.GlobalMarketPoint
import io.horizontalsystems.marketkit.models.HsPeriodType
import java.math.BigDecimal

class DatabaseTypeConverters {
    private val gson by lazy { Gson() }

    @TypeConverter
    fun fromMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toMap(value: String): Map<String, String> {
        return gson.fromJson(value, object : TypeToken<Map<String, String>>() {}.type)
    }

    @TypeConverter
    fun fromBigDecimal(bigDecimal: BigDecimal?): String? {
        return bigDecimal?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(value) }
    }

    @TypeConverter
    fun fromHsPeriodType(periodType: HsPeriodType): String {
        return periodType.serialize()
    }

    @TypeConverter
    fun toHsPeriodType(value: String): HsPeriodType? {
        return HsPeriodType.deserialize(value)
    }

    @TypeConverter
    fun fromGlobalMarketPointList(list: List<GlobalMarketPoint>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toGlobalMarketPointList(value: String): List<GlobalMarketPoint> {
        return gson.fromJson(value, object : TypeToken<List<GlobalMarketPoint>>() {}.type)
    }
}
