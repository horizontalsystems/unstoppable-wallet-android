package io.horizontalsystems.bankwallet.core.storage

import androidx.room.TypeConverter
import io.horizontalsystems.bankwallet.core.App
import java.math.BigDecimal

class DatabaseConverters {

    // BigDecimal

    @TypeConverter
    fun fromString(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }

    @TypeConverter
    fun toString(bigDecimal: BigDecimal?): String? {
        return bigDecimal?.toPlainString()
    }

    // List<String>

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")
    }

    @TypeConverter
    fun toString(value: List<String>?): String? {
        return value?.joinToString(separator = ",")
    }

    // SecretString

    @TypeConverter
    fun decryptSecretString(value: String?): SecretString? {
        return value?.let {
            SecretString(App.encryptionManager.decrypt(it))
        }
    }

    @TypeConverter
    fun encryptSecretString(secretString: SecretString?): String? {
        return secretString?.value?.let { App.encryptionManager.encrypt(it) }
    }

    // SecretList

    @TypeConverter
    fun decryptSecretList(value: String?): SecretList? {
        return value?.let {
            SecretList(App.encryptionManager.decrypt(it).split(","))
        }
    }

    @TypeConverter
    fun encryptSecretList(secretList: SecretList?): String? {
        return secretList?.list?.joinToString(separator = ",")?.let {
            App.encryptionManager.encrypt(it)
        }
    }
}
