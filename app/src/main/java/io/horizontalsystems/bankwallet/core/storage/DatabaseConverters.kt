package io.horizontalsystems.bankwallet.core.storage

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.CoinType
import java.math.BigDecimal
import java.util.*

class DatabaseConverters {

    private val gson by lazy { Gson() }

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
    fun fromCoinType(coinType: CoinType?): String {
        return coinType?.id ?: ""
    }

    @TypeConverter
    fun toCoinType(value: String): CoinType {
        return CoinType.fromId(value)
    }

    @TypeConverter
    fun fromWCPeerMeta(peerMeta: WCPeerMeta): String {
        return gson.toJson(peerMeta)
    }

    @TypeConverter
    fun toWCPeerMeta(json: String): WCPeerMeta {
        return gson.fromJson(json, WCPeerMeta::class.java)
    }

    @TypeConverter
    fun fromWCSession(session: WCSession): String {
        return gson.toJson(session)
    }

    @TypeConverter
    fun toWCSession(json: String): WCSession {
        return gson.fromJson(json, WCSession::class.java)
    }

    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }

}
