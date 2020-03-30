package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.ISyncModeSettingsManager
import io.horizontalsystems.bankwallet.entities.*

class BlockchainSettingsManager(
        private val derivationSettingsManager: IDerivationSettingsManager,
        private val syncModeSettingsManager: ISyncModeSettingsManager,
        private val appConfigProvider: IAppConfigProvider) : IBlockchainSettingsManager {

    override val coinsWithSettings: List<Coin>
        get() {
            val coinTypesWithDerivationSetting = appConfigProvider.derivationSettings.map { it.coinType }
            val coinTypesWithSyncModeSetting = appConfigProvider.syncModeSettings.map { it.coinType }
            val allCoinTypes = coinTypesWithDerivationSetting.union(coinTypesWithSyncModeSetting)

            return allCoinTypes.map { coinType ->
                appConfigProvider.coins.first { it.type == coinType }
            }
        }

    override fun derivationSetting(coinType: CoinType, forCreate: Boolean): DerivationSetting? {
        val default = derivationSettingsManager.defaultDerivationSetting(coinType)
        if (forCreate) {
            return default
        }
        return derivationSettingsManager.derivationSetting(coinType) ?: default
    }

    override fun syncModeSetting(coinType: CoinType, forCreate: Boolean): SyncModeSetting? {
        val default = syncModeSettingsManager.defaultSyncModeSetting(coinType)
        if (forCreate) {
            default?.syncMode = SyncMode.New
            return default
        }
        return syncModeSettingsManager.syncModeSetting(coinType) ?: default
    }

    override fun updateSetting(derivationSetting: DerivationSetting) {
        derivationSettingsManager.updateSetting(derivationSetting)
    }

    override fun updateSetting(syncModeSetting: SyncModeSetting) {
        syncModeSettingsManager.updateSetting(syncModeSetting)
    }

}
