package io.horizontalsystems.stellarkit.room

import androidx.room.TypeConverter
import java.math.BigDecimal

interface Converter<T> {
    fun fromString(s: String?): T?
    fun toString(v: T?): String?
}

class Converters {
    // BigDecimal

    @TypeConverter
    fun fromString(value: String?): BigDecimal? = try {
        value?.let { BigDecimal(it) }
    } catch (e: Exception) {
        null
    }

    @TypeConverter
    fun toString(bigDecimal: BigDecimal?): String? {
        return bigDecimal?.toPlainString()
    }
}

class StellarAssetConverter {
    @TypeConverter
    fun fromString(value: String?): StellarAsset? = try {
        value?.let { StellarAsset.fromId(it) }
    } catch (e: Exception) {
        null
    }

    @TypeConverter
    fun toString(v: StellarAsset?) = v?.id

}

class ConveterListOfStrings : Converter<List<String>> {
    @TypeConverter
    override fun fromString(s: String?) = s?.split("|")

    @TypeConverter
    override fun toString(v: List<String>?) = v?.joinToString("|")
}
