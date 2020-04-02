package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.ICommunicationSettingsManager
import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.ISyncModeSettingsManager
import io.horizontalsystems.bankwallet.entities.*

class BlockchainSettingsManager(
        private val derivationSettingsManager: IDerivationSettingsManager,
        private val syncModeSettingsManager: ISyncModeSettingsManager,
        private val communicationSettingsManager: ICommunicationSettingsManager
) : IBlockchainSettingsManager {

    override fun derivationSetting(coinType: CoinType, forCreate: Boolean): DerivationSetting? {
        val defaultDerivationSetting: DerivationSetting? = try {
            derivationSettingsManager.defaultDerivationSetting(coinType)
        } catch (e: NoSuchElementException) {
            null
        }

        if (forCreate) {
            return defaultDerivationSetting
        }
        return derivationSettingsManager.derivationSetting(coinType) ?: defaultDerivationSetting
    }

    override fun syncModeSetting(coinType: CoinType, forCreate: Boolean): SyncModeSetting? {
        val default = syncModeSettingsManager.defaultSyncModeSetting(coinType)
        if (forCreate) {
            default?.syncMode = SyncMode.New
            return default
        }
        return syncModeSettingsManager.syncModeSetting(coinType) ?: default
    }

    override fun communicationSetting(coinType: CoinType, forCreate: Boolean): CommunicationSetting? {
        val default = communicationSettingsManager.defaultCommunicationSetting(coinType)
        if (forCreate) {
            return default
        }
        return communicationSettingsManager.communicationSetting(coinType) ?: default
    }

    override fun updateSetting(derivationSetting: DerivationSetting) {
        derivationSettingsManager.updateSetting(derivationSetting)
    }

    override fun updateSetting(syncModeSetting: SyncModeSetting) {
        syncModeSettingsManager.updateSetting(syncModeSetting)
    }

    override fun updateSetting(communicationSetting: CommunicationSetting) {
        communicationSettingsManager.updateSetting(communicationSetting)
    }
}
