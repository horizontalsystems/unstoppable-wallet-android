package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IRestoreSettingsStorage
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.RestoreSettingRecord
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType

class RestoreSettingsManager(
        private val storage: IRestoreSettingsStorage,
        private val zcashBirthdayProvider: ZcashBirthdayProvider
) {
    fun settings(account: Account, coin: io.horizontalsystems.marketkit.models.CoinType): RestoreSettings {
        val records = storage.restoreSettings(account.id, coin.id)

        val settings = RestoreSettings()
        records.forEach { record ->
            RestoreSettingType.fromString(record.key)?.let { type ->
                settings[type] = record.value
            }
        }

        return settings
    }

    fun accountSettingsInfo(account: Account): List<Triple<CoinType, RestoreSettingType, String>> {
        return storage.restoreSettings(account.id).mapNotNull { record ->
            RestoreSettingType.fromString(record.key)?.let { settingType ->
                val coinType = CoinType.fromString(record.coinId)
                Triple(coinType, settingType, record.value)
            }
        }
    }

    fun save(settings: RestoreSettings, account: Account, coin: Coin) {
        val records = settings.values.map { (type, value) ->
            RestoreSettingRecord(account.id, coin.id, type.name, value)
        }

        storage.save(records)
    }

    fun getSettingValueForCreatedAccount(settingType: RestoreSettingType, coinType: CoinType): String? {
        return when (settingType) {
            RestoreSettingType.BirthdayHeight -> {
                when (coinType) {
                    CoinType.Zcash -> {
                        return zcashBirthdayProvider.getNearestBirthdayHeight().toString()
                    }
                    else -> null
                }
            }
        }
    }

    fun getSettingsTitle(settingType: RestoreSettingType, coin: Coin): String {
        return when (settingType) {
            RestoreSettingType.BirthdayHeight -> Translator.getString(R.string.ManageAccount_BirthdayHeight, coin.code)
        }
    }

}

enum class RestoreSettingType {
    BirthdayHeight;

    companion object {
        private val map = values().associateBy(RestoreSettingType::name)

        fun fromString(value: String?): RestoreSettingType? = map[value]
    }
}

class RestoreSettings {
    val values = mutableMapOf<RestoreSettingType, String>()

    var birthdayHeight: Int?
        get() = values[RestoreSettingType.BirthdayHeight]?.toIntOrNull()
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
