package cash.p.terminal.core.storage.typeconverter

import androidx.room.TypeConverter
import cash.p.terminal.core.App
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.wallet.entities.HardwarePublicKeyType
import cash.p.terminal.wallet.entities.SecretList
import cash.p.terminal.wallet.entities.SecretString
import cash.p.terminal.wallet.entities.TokenType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.core.entities.BlockchainType
import java.math.BigDecimal
import java.util.Date

class DatabaseConverters {

    private val gson by lazy { Gson() }

    // TokenType
    @TypeConverter
    fun fromTokenType(value: String?): TokenType? {
        return value?.let { TokenType.fromId(value) }
    }

    @TypeConverter
    fun toTokenType(value: TokenType?): String? {
        return value?.id
    }

    // HardwarePublicKeyType
    @TypeConverter
    fun fromHardwarePublicKeyType(value: Int?): HardwarePublicKeyType? {
        return value?.let { HardwarePublicKeyType.entries.getOrNull(it) }
    }

    @TypeConverter
    fun toHardwarePublicKeyType(value: HardwarePublicKeyType?): Int? {
        return value?.ordinal
    }

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

    // SecretString

    @TypeConverter
    fun decryptSecretString(value: String?): SecretString? {
        if (value == null) return null

        return try {
            SecretString(App.Companion.encryptionManager.decrypt(value))
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun encryptSecretString(secretString: SecretString?): String? {
        return secretString?.value?.let { App.Companion.encryptionManager.encrypt(it) }
    }

    // SecretList

    @TypeConverter
    fun decryptSecretList(value: String?): SecretList? {
        if (value == null) return null

        return try {
            SecretList(App.Companion.encryptionManager.decrypt(value).split(","))
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun encryptSecretList(secretList: SecretList?): String? {
        return secretList?.list?.joinToString(separator = ",")?.let {
            App.Companion.encryptionManager.encrypt(it)
        }
    }

    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }

    @TypeConverter
    fun fromBlockchainType(blockchainType: BlockchainType): String {
        return blockchainType.uid
    }

    @TypeConverter
    fun toBlockchainType(string: String): BlockchainType {
        return BlockchainType.Companion.fromUid(string)
    }

    @TypeConverter
    fun fromNftUid(nftUid: NftUid): String {
        return nftUid.uid
    }

    @TypeConverter
    fun toNftUid(string: String): NftUid {
        return NftUid.Companion.fromUid(string)
    }

    @TypeConverter
    fun fromMap(v: Map<String, String?>): String {
        return gson.toJson(v)
    }

    @TypeConverter
    fun toMap(v: String): Map<String, String?> {
        return gson.fromJson(v, object : TypeToken<Map<String, String?>>() {}.type)
    }
}