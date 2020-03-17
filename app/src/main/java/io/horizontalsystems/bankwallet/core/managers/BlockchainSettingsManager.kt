package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.*

class BlockchainSettingsManager(
        private val database: AppDatabase,
        private val appConfigProvider: IAppConfigProvider) : IBlockchainSettingsManager {

    override val defaultBlockchainSettings: List<BlockchainSetting>
        get() = appConfigProvider.blockchainSettings

    override val coinsWithSettings: List<Coin>
        get() {
            return appConfigProvider.blockchainSettings.map { setting ->
                appConfigProvider.coins.first { it.type == setting.coinType }
            }
        }

    override fun blockchainSettingsForCreate(coinType: CoinType): BlockchainSetting? {
        val settings = appConfigProvider.blockchainSettings.firstOrNull { it.coinType == coinType }
                ?: return null
        settings.syncMode = SyncMode.New
        return settings
    }

    override fun blockchainSettings(coinType: CoinType): BlockchainSetting? {
        return database.blockchainSettingDao().getSetting(coinType)
                ?: appConfigProvider.blockchainSettings.firstOrNull { it.coinType == coinType }
    }

    override fun updateSettings(blockchainSettings: BlockchainSetting) {
        database.blockchainSettingDao().insert(blockchainSettings)
    }

}
