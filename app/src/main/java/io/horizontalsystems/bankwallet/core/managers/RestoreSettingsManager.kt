package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IRestoreSettingsStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.RestoreSettingRecord
import io.horizontalsystems.coinkit.models.Coin

class RestoreSettingsManager(private val storage: IRestoreSettingsStorage) {
    fun settings(account: Account, coin: Coin): RestoreSettings {
        val records = storage.restoreSettings(account.id, coin.id)

        val settings = RestoreSettings()
        records.forEach { record ->
            RestoreSettingType.fromString(record.key)?.let { type ->
                settings[type] = record.value
            }
        }

        return settings
    }

    fun save(settings: RestoreSettings, account: Account, coin: Coin) {
        val records = settings.values.map { (type, value) ->
            RestoreSettingRecord(account.id, coin.id, type.name, value)
        }

        storage.save(records)
    }
}

enum class RestoreSettingType {
    birthdayHeight;

    companion object {
        private val map = values().associateBy(RestoreSettingType::name)

        fun fromString(value: String?): RestoreSettingType? = map[value]
    }
}

class RestoreSettings {
    val values = mutableMapOf<RestoreSettingType, String>()

    var birthdayHeight: Int?
        get() = values[RestoreSettingType.birthdayHeight]?.toIntOrNull()
        set(value) {
            values[RestoreSettingType.birthdayHeight] = value?.toString() ?: ""
        }

    fun isNotEmpty() = values.isNotEmpty()

    operator fun get(key: RestoreSettingType): String? {
        return values[key]
    }

    operator fun set(key: RestoreSettingType, value: String) {
        values[key] = value
    }
}
