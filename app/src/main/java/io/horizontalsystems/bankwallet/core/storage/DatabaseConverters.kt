package io.horizontalsystems.bankwallet.core.storage

import androidx.room.TypeConverter
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CoinType
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
        if (value == null) return null

        return try {
            SecretString(App.encryptionManager.decrypt(value))
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun encryptSecretString(secretString: SecretString?): String? {
        return secretString?.value?.let { App.encryptionManager.encrypt(it) }
    }

    // SecretList

    @TypeConverter
    fun decryptSecretList(value: String?): SecretList? {
        if (value == null) return null

        return try {
            SecretList(App.encryptionManager.decrypt(value).split(","))
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun encryptSecretList(secretList: SecretList?): String? {
        return secretList?.list?.joinToString(separator = ",")?.let {
            App.encryptionManager.encrypt(it)
        }
    }

    @TypeConverter
    fun fromSyncMode(syncMode: SyncMode?): String? {
        return syncMode?.value
    }

    @TypeConverter
    fun toSyncMode(value: String?): SyncMode? {
        return value?.let { SyncMode.valueOf(it) }
    }

    @TypeConverter
    fun fromDerivation(derivation: AccountType.Derivation?): String? {
        return derivation?.value
    }

    @TypeConverter
    fun toDerivation(value: String?): AccountType.Derivation? {
        if (value == null) return null

        return AccountType.Derivation.valueOf(value)
    }

    @TypeConverter
    fun fromCoinType(coinType: CoinType): String {
        return when(coinType) {
            CoinType.Bitcoin -> bitcoin
            CoinType.Litecoin -> litecoin
            CoinType.BitcoinCash -> bitcoinCash
            CoinType.Dash -> dash
            CoinType.Ethereum -> ethereum
            else -> ""
        }
    }

    @TypeConverter
    fun toCoinType(value: String): CoinType? {
        return when (value) {
            bitcoin -> CoinType.Bitcoin
            litecoin -> CoinType.Litecoin
            bitcoinCash -> CoinType.BitcoinCash
            dash -> CoinType.Dash
            ethereum -> CoinType.Ethereum
            else -> null
        }
    }

    companion object {
        const val bitcoin = "bitcoin"
        const val litecoin = "litecoin"
        const val bitcoinCash = "bitcoincash"
        const val dash = "dash"
        const val ethereum = "ethereum"
    }

}
