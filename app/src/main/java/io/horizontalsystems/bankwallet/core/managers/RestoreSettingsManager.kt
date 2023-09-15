package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IRestoreSettingsStorage
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.RestoreSettingRecord
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

class RestoreSettingsManager(
        private val storage: IRestoreSettingsStorage,
        private val zcashBirthdayProvider: ZcashBirthdayProvider
) {
    fun settings(account: Account, blockchainType: BlockchainType): RestoreSettings {
        val records = storage.restoreSettings(account.id, blockchainType.uid)

        val settings = RestoreSettings()
        records.forEach { record ->
            RestoreSettingType.fromString(record.key)?.let { type ->
                settings[type] = record.value
            }
        }

        return settings
    }

    fun accountSettingsInfo(account: Account): List<Triple<BlockchainType, RestoreSettingType, String>> {
        return storage.restoreSettings(account.id).mapNotNull { record ->
            RestoreSettingType.fromString(record.key)?.let { settingType ->
                val blockchainType = BlockchainType.fromUid(record.blockchainTypeUid)
                Triple(blockchainType, settingType, record.value)
            }
        }
    }

    fun save(settings: RestoreSettings, account: Account, blockchainType: BlockchainType) {
        val records = settings.values.map { (type, value) ->
            RestoreSettingRecord(account.id, blockchainType.uid, type.name, value)
        }

        storage.save(records)
    }

    fun getSettingValueForCreatedAccount(settingType: RestoreSettingType, blockchainType: BlockchainType): String? {
        return when (settingType) {
            RestoreSettingType.BirthdayHeight -> {
                when (blockchainType) {
                    BlockchainType.Zcash -> {
                        return zcashBirthdayProvider.getLatestCheckpointBlockHeight().toString()
                    }
                    else -> null
                }
            }
        }
    }

    fun getSettingsTitle(settingType: RestoreSettingType, token: Token): String {
        return when (settingType) {
            RestoreSettingType.BirthdayHeight -> Translator.getString(R.string.ManageAccount_BirthdayHeight, token.coin.code)
        }
    }

}

enum class RestoreSettingType {
    @SerializedName("birthday_height")
    BirthdayHeight;

    companion object {
        private val map = values().associateBy(RestoreSettingType::name)

        fun fromString(value: String?): RestoreSettingType? = map[value]
    }
}

class RestoreSettings {
    val values = mutableMapOf<RestoreSettingType, String>()

    var birthdayHeight: Long?
        get() = values[RestoreSettingType.BirthdayHeight]?.toLongOrNull()
        set(value) {
            values[RestoreSettingType.BirthdayHeight] = value?.toString() ?: ""
        }

    fun isNotEmpty() = values.isNotEmpty()

    operator fun get(key: RestoreSettingType): String? {
        return values[key]
    }

    operator fun set(key: RestoreSettingType, value: String) {
        values[key] = value
    }
}
