package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BlockchainSetting
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting

class DerivationSettingsManager(
        private val appConfigProvider: IAppConfigProvider,
        private val appDatabase: AppDatabase
) : IDerivationSettingsManager {

    companion object {
        const val derivationSettingKey: String = "derivation"
    }

    override fun defaultDerivationSetting(coinType: CoinType): DerivationSetting? {
        return appConfigProvider.derivationSettings.firstOrNull{ it.coinType == coinType }
    }

    override fun derivationSetting(coinType: CoinType): DerivationSetting? {
        val blockchainSetting = appDatabase.blockchainSettingDao().getSetting(coinType, derivationSettingKey)
        return blockchainSetting?.let { DerivationSetting(coinType, AccountType.Derivation.valueOf(it.value)) } ?: defaultDerivationSetting(coinType)
    }

    override fun updateSetting(derivationSetting: DerivationSetting) {
        appDatabase.blockchainSettingDao().insert(BlockchainSetting(derivationSetting.coinType, derivationSettingKey, derivationSetting.derivation.value))
    }

    override fun reset() {
        appDatabase.blockchainSettingDao().deleteAll()
    }
}
