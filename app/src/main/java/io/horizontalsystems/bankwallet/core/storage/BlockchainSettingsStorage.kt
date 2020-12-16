package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IBlockchainSettingsStorage
import io.horizontalsystems.bankwallet.entities.*

class BlockchainSettingsStorage(private val appDatabase: AppDatabase): IBlockchainSettingsStorage {

    companion object {
        const val syncModeSettingKey: String = "sync_mode"
        const val derivationSettingKey: String = "derivation"
        const val ethereumRpcModeSettingKey: String = "communication"
    }

    override fun derivationSetting(coinType: CoinType): DerivationSetting? {
        val blockchainSetting = appDatabase.blockchainSettingDao().getSetting(coinType, derivationSettingKey)
        return blockchainSetting?.let { DerivationSetting(coinType, AccountType.Derivation.valueOf(it.value)) }
    }

    override fun saveDerivationSetting(derivationSetting: DerivationSetting) {
        appDatabase.blockchainSettingDao().insert(BlockchainSetting(derivationSetting.coinType, derivationSettingKey, derivationSetting.derivation.value))
    }

    override fun deleteDerivationSettings() {
        appDatabase.blockchainSettingDao().deleteDerivationSettings(derivationSettingKey)
    }

    override fun initialSyncSetting(coinType: CoinType): InitialSyncSetting? {
        val blockchainSetting = appDatabase.blockchainSettingDao().getSetting(coinType, syncModeSettingKey)
        return blockchainSetting?.let { InitialSyncSetting(coinType, SyncMode.valueOf(it.value)) }
    }

    override fun saveInitialSyncSetting(initialSyncSetting: InitialSyncSetting) {
        appDatabase.blockchainSettingDao().insert(BlockchainSetting(initialSyncSetting.coinType, syncModeSettingKey, initialSyncSetting.syncMode.value))
    }

    override fun ethereumRpcModeSetting(coinType: CoinType): EthereumRpcMode? {
        val setting = appDatabase.blockchainSettingDao().getSetting(coinType, ethereumRpcModeSettingKey)
        return setting?.let { EthereumRpcMode(coinType, CommunicationMode.valueOf(it.value)) }
    }

    override fun saveEthereumRpcModeSetting(ethereumRpcModeSetting: EthereumRpcMode) {
        appDatabase.blockchainSettingDao().insert(BlockchainSetting(ethereumRpcModeSetting.coinType, syncModeSettingKey, ethereumRpcModeSetting.communicationMode.value))
    }
}
