package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin

class InitialSyncSettingsManager(
        private val coinManager: ICoinManager,
        private val blockchainSettingsStorage: IBlockchainSettingsStorage,
        private val adapterManager: IAdapterManager,
        private val walletManager: IWalletManager,
        private val marketKit: MarketKit
) : IInitialSyncModeSettingsManager {

    private val supportedCoinTypes = listOf(
            SupportedCoinType(CoinType.Bitcoin, SyncMode.Fast, true),
            SupportedCoinType(CoinType.BitcoinCash, SyncMode.Fast, true),
            SupportedCoinType(CoinType.Dash, SyncMode.Fast, true),
            SupportedCoinType(CoinType.Litecoin, SyncMode.Fast, true)
    )

    override fun allSettings(): List<Triple<InitialSyncSetting, PlatformCoin, Boolean>> {
        val platformCoins = marketKit.platformCoins()

        return supportedCoinTypes.mapNotNull { supportedCoinType ->
            val coinTypePlatformCoin = platformCoins.find { it.coinType == supportedCoinType.coinType } ?: return@mapNotNull null

            val setting = setting(supportedCoinType.coinType) ?: return@mapNotNull null

            Triple(setting, coinTypePlatformCoin, supportedCoinType.changeable)
        }
    }

    override fun setting(coinType: CoinType, origin: AccountOrigin?): InitialSyncSetting? {
        val supportedCoinType = supportedCoinTypes.firstOrNull{ it.coinType == coinType } ?: return null

        if (origin == AccountOrigin.Created){
            return InitialSyncSetting(coinType, SyncMode.New)
        }

        if(!supportedCoinType.changeable){
            return defaultSetting(supportedCoinType.coinType)
        }

        val storedSetting = blockchainSettingsStorage.initialSyncSetting(coinType)

        return storedSetting ?: defaultSetting(coinType)
    }

    override fun save(setting: InitialSyncSetting) {
        blockchainSettingsStorage.saveInitialSyncSetting(setting)

        val walletsForUpdate = walletManager.activeWallets.filter { it.coinType == setting.coinType && it.account.origin == AccountOrigin.Restored }

        if (walletsForUpdate.isNotEmpty()) {
            adapterManager.refreshAdapters(walletsForUpdate)
        }
    }

    private fun defaultSetting(supportedCoinType: CoinType): InitialSyncSetting {
        return InitialSyncSetting(supportedCoinType, supportedCoinTypes.first{ it.coinType == supportedCoinType }.defaultSyncMode)
    }

    private data class SupportedCoinType (
            val coinType: CoinType,
            val defaultSyncMode: SyncMode,
            val changeable : Boolean
    )

}
