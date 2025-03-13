package io.horizontalsystems.bankwallet.core.storage

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.CexDepositNetworkRaw
import io.horizontalsystems.bankwallet.core.providers.CexWithdrawNetworkRaw
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal
import java.util.Date

class DatabaseConverters {
    private val gson by lazy { Gson() }

    // BigDecimal

    @TypeConverter
    fun fromString(value: String?): BigDecimal? =
        try {
            value?.let { BigDecimal(it) }
        } catch (e: Exception) {
            null
        }

    @TypeConverter
    fun toString(bigDecimal: BigDecimal?): String? = bigDecimal?.toPlainString()

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
    fun encryptSecretString(secretString: SecretString?): String? = secretString?.value?.let { App.encryptionManager.encrypt(it) }

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
    fun encryptSecretList(secretList: SecretList?): String? =
        secretList?.list?.joinToString(separator = ",")?.let {
            App.encryptionManager.encrypt(it)
        }

    @TypeConverter
    fun fromDate(date: Date): Long = date.time

    @TypeConverter
    fun toDate(timestamp: Long): Date = Date(timestamp)

    @TypeConverter
    fun fromBlockchainType(blockchainType: BlockchainType): String = blockchainType.uid

    @TypeConverter
    fun toBlockchainType(string: String): BlockchainType = BlockchainType.fromUid(string)

    @TypeConverter
    fun fromNftUid(nftUid: NftUid): String = nftUid.uid

    @TypeConverter
    fun toNftUid(string: String): NftUid = NftUid.fromUid(string)

    @TypeConverter
    fun fromCexDepositNetworkList(networks: List<CexDepositNetworkRaw>): String = gson.toJson(networks)

    @TypeConverter
    fun toCexDepositNetworkList(json: String): List<CexDepositNetworkRaw>? =
        gson.fromJson(
            json,
            object : TypeToken<List<CexDepositNetworkRaw>>() {}.type,
        )

    @TypeConverter
    fun fromCexWithdrawNetworkList(networks: List<CexWithdrawNetworkRaw>): String = gson.toJson(networks)

    @TypeConverter
    fun toCexWithdrawNetworkList(json: String): List<CexWithdrawNetworkRaw>? =
        gson.fromJson(
            json,
            object : TypeToken<List<CexWithdrawNetworkRaw>>() {}.type,
        )

    @TypeConverter
    fun fromMap(v: Map<String, String?>): String = gson.toJson(v)

    @TypeConverter
    fun toMap(v: String): Map<String, String?> = gson.fromJson(v, object : TypeToken<Map<String, String?>>() {}.type)
}
