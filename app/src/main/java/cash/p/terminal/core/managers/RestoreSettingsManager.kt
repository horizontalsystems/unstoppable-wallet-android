package cash.p.terminal.core.managers

import com.google.gson.annotations.SerializedName
import cash.p.terminal.R
import cash.p.terminal.core.IRestoreSettingsStorage
import cash.p.terminal.entities.RestoreSettingRecord
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.BlockchainType

class RestoreSettingsManager(
        private val storage: IRestoreSettingsStorage,
        private val zcashBirthdayProvider: ZcashBirthdayProvider
) {
    fun settings(account: cash.p.terminal.wallet.Account, blockchainType: BlockchainType): RestoreSettings {
        val records = storage.restoreSettings(account.id, blockchainType.uid)

        val settings = RestoreSettings()
        records.forEach { record ->
            RestoreSettingType.fromString(record.key)?.let { type ->
                settings[type] = record.value
            }
        }

        return settings
    }

    fun accountSettingsInfo(account: cash.p.terminal.wallet.Account): List<Triple<BlockchainType, RestoreSettingType, String>> {
        return storage.restoreSettings(account.id).mapNotNull { record ->
            RestoreSettingType.fromString(record.key)?.let { settingType ->
                val blockchainType = BlockchainType.fromUid(record.blockchainTypeUid)
                Triple(blockchainType, settingType, record.value)
            }
        }
    }

    fun save(settings: RestoreSettings, account: cash.p.terminal.wallet.Account, blockchainType: BlockchainType) {
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
            RestoreSettingType.BirthdayHeight -> cash.p.terminal.strings.helpers.Translator.getString(R.string.ManageAccount_BirthdayHeight, token.coin.code)
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
