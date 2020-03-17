package io.horizontalsystems.bankwallet.modules.blockchainsettingslist

import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.entities.BlockchainSetting
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

class BlockchainSettingsListInteractor(
        private val blockchainSettingsManager: IBlockchainSettingsManager
) : BlockchainSettingsListModule.IInteractor {

    override val coinsWithSettings: List<Coin>
        get() = blockchainSettingsManager.coinsWithSettings

    override fun blockchainSettings(coinType: CoinType): BlockchainSetting? {
        return blockchainSettingsManager.blockchainSettings(coinType)
    }
}
