package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ISyncModeSettingsManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.BlockchainSetting
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.SyncModeSetting

class SyncModeSettingsManager(
        private val appConfigProvider: IAppConfigProvider,
        private val appDatabase: AppDatabase
) : ISyncModeSettingsManager {

    companion object {
        const val syncModeSettingKey: String = "sync_mode"
    }

    override fun defaultSyncModeSetting(coinType: CoinType): SyncModeSetting? {
        return appConfigProvider.syncModeSettings.firstOrNull { it.coinType == coinType }
    }

    override fun syncModeSetting(coinType: CoinType): SyncModeSetting? {
        val blockchainSetting = appDatabase.blockchainSettingDao().getSetting(coinType, syncModeSettingKey)
        return blockchainSetting?.let { SyncModeSetting(coinType, SyncMode.valueOf(it.value)) } ?: defaultSyncModeSetting(coinType)
    }

    override fun updateSetting(syncModeSetting: SyncModeSetting) {
        appDatabase.blockchainSettingDao().insert(BlockchainSetting(syncModeSetting.coinType, syncModeSettingKey, syncModeSetting.syncMode.value))
    }

}
