package io.horizontalsystems.bankwallet.core.storage

import androidx.room.TypeConverter
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
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

    // SyncMode

    @TypeConverter
    fun toString(syncMode: SyncMode?): String? {
        return syncMode?.value
    }

    @TypeConverter
    fun toSyncMode(string: String?): SyncMode? {
        return SyncMode.fromString(string)
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

    // Derivation

    @TypeConverter
    fun toDerivation(value: String?): AccountType.Derivation? {
        return value?.let { AccountType.Derivation.valueOf(it) }
    }

    @TypeConverter
    fun toString(derivation: AccountType.Derivation?): String? {
        return derivation?.toString()
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
