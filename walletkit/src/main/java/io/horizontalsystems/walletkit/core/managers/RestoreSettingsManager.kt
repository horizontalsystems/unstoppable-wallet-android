package io.horizontalsystems.walletkit.core.managers

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.IRestoreSettingsStorage
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.RestoreSettingRecord
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class RestoreSettingsManager(
    private val storage: IRestoreSettingsStorage
) {
    private val _settingsUpdatedFlow = MutableSharedFlow<BlockchainType>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val settingsUpdatedFlow = _settingsUpdatedFlow.asSharedFlow()

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

    fun save(settings: RestoreSettings, account: Account, blockchainType: BlockchainType, reload: Boolean = true) {
        val records = settings.values.map { (type, value) ->
            RestoreSettingRecord(account.id, blockchainType.uid, type.name, value)
        }

        storage.save(records)
        // reload = false when ENABLING/CREATING a wallet: the wallet is created
        // right after with these settings, so emitting here makes WalletManager
        // spuriously reloadWallets() and churn the just-created adapter (two
        // MoneroKit instances racing -> stuck Connecting / false-synced). Only a
        // settings CHANGE on an already-running wallet needs the reload.
        if (reload) {
            _settingsUpdatedFlow.tryEmit(blockchainType)
        }
    }

    fun getSettingValueForCreatedAccount(settingType: RestoreSettingType, blockchainType: BlockchainType): String? {
        return when (settingType) {
            RestoreSettingType.BirthdayHeight -> {
                when (blockchainType) {
                    else -> ChainRegistry[blockchainType]?.newWalletBirthdayHeight()?.toString()
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
            values[RestoreSettingType.BirthdayHeight] = value?.toString() ?: "0"
        }

    fun isNotEmpty() = values.isNotEmpty()

    operator fun get(key: RestoreSettingType): String? {
        return values[key]
    }

    operator fun set(key: RestoreSettingType, value: String) {
        values[key] = value
    }
}
