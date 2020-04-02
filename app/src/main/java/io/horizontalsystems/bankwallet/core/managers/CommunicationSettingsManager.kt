package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ICommunicationSettingsManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.BlockchainSetting
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.CommunicationMode
import io.horizontalsystems.bankwallet.entities.CommunicationSetting

class CommunicationSettingsManager(
        private val appConfigProvider: IAppConfigProvider,
        private val appDatabase: AppDatabase
) : ICommunicationSettingsManager {

    companion object {
        const val communicationSettingKey: String = "communication"
    }

    override fun defaultCommunicationSetting(coinType: CoinType): CommunicationSetting? {
        return appConfigProvider.communicationSettings.firstOrNull { it.coinType.javaClass == coinType.javaClass }
    }

    override fun communicationSetting(coinType: CoinType): CommunicationSetting? {
        val blockchainSetting = appDatabase.blockchainSettingDao().getSetting(coinType, communicationSettingKey)
        return blockchainSetting?.let { CommunicationSetting(coinType, CommunicationMode.valueOf(it.value)) }
    }

    override fun updateSetting(communicationSetting: CommunicationSetting) {
        appDatabase.blockchainSettingDao().insert(BlockchainSetting(communicationSetting.coinType, communicationSettingKey, communicationSetting.communicationMode.value))
    }

}
