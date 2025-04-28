package io.horizontalsystems.stellarkit.room

import androidx.room.TypeConverter
import java.math.BigDecimal

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
