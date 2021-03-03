package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.coinkit.models.CoinType

class DerivationSettingsManager(
        private val blockchainSettingsStorage: IBlockchainSettingsStorage,
        private val adapterManager: IAdapterManager,
        private val walletManager: IWalletManager) : IDerivationSettingsManager {

    private val supportedCoinTypes = mapOf(
            CoinType.Bitcoin to AccountType.Derivation.bip49,
            CoinType.Litecoin to AccountType.Derivation.bip49
    )

    override fun allActiveSettings(): List<Pair<DerivationSetting, CoinType>> {
        val wallets = walletManager.wallets

        return supportedCoinTypes.mapNotNull { (coinType, _) ->
            wallets.firstOrNull { it.coin.type == coinType }?.coin ?: return@mapNotNull null
            val setting = setting(coinType) ?: return@mapNotNull null
            Pair(setting, coinType)
        }
    }

    override fun defaultSetting(coinType: CoinType): DerivationSetting? {
        val defaultDerivation = supportedCoinTypes[coinType] ?: return null
        return DerivationSetting(coinType, defaultDerivation)
    }

    override fun setting(coinType: CoinType): DerivationSetting? {
        return blockchainSettingsStorage.derivationSetting(coinType)
                ?: defaultSetting(coinType)
    }

    override fun save(setting: DerivationSetting) {
        blockchainSettingsStorage.saveDerivationSetting(setting)

        val walletsForUpdate = walletManager.wallets.filter { it.coin.type == setting.coinType }

        if (walletsForUpdate.isNotEmpty()) {
            adapterManager.refreshAdapters(walletsForUpdate)
        }
    }

    override fun resetStandardSettings() {
        blockchainSettingsStorage.deleteDerivationSettings()
    }
}
