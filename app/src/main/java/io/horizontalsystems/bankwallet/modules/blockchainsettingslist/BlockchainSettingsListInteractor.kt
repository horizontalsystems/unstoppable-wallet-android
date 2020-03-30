package io.horizontalsystems.bankwallet.modules.blockchainsettingslist

import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.SyncMode

class BlockchainSettingsListInteractor(
        private val blockchainSettingsManager: IBlockchainSettingsManager
) : BlockchainSettingsListModule.IInteractor {

    override val coinsWithSettings: List<Coin>
        get() = blockchainSettingsManager.coinsWithSettings

    override fun derivation(coinType: CoinType): Derivation? {
        return blockchainSettingsManager.derivationSetting(coinType)?.derivation
    }

    override fun syncMode(coinType: CoinType): SyncMode? {
        return blockchainSettingsManager.syncModeSetting(coinType)?.syncMode
    }
}
