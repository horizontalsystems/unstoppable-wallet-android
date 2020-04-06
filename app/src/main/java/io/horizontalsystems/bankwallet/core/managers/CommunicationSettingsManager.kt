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

    private fun getBaseCoinType(coinType: CoinType): CoinType {
        return when (coinType) {
            is CoinType.Erc20 -> CoinType.Ethereum
            else -> coinType
        }
    }

    override fun defaultCommunicationSetting(coinType: CoinType): CommunicationSetting? {
        return appConfigProvider.communicationSettings.firstOrNull { it.coinType.javaClass == getBaseCoinType(coinType).javaClass }
    }

    override fun communicationSetting(coinType: CoinType): CommunicationSetting? {
        val blockchainSetting = appDatabase.blockchainSettingDao().getSetting(getBaseCoinType(coinType), communicationSettingKey)
        return blockchainSetting?.let { CommunicationSetting(getBaseCoinType(coinType), CommunicationMode.valueOf(it.value)) }
    }

    override fun updateSetting(communicationSetting: CommunicationSetting) {
        appDatabase.blockchainSettingDao().insert(BlockchainSetting(getBaseCoinType(communicationSetting.coinType), communicationSettingKey, communicationSetting.communicationMode.value))
    }

}
