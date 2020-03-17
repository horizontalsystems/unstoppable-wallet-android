package io.horizontalsystems.bankwallet.modules.blockchainsettings

import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.*

class BlockchainSettingsInteractor(
        private val blockchainSettingsManager: IBlockchainSettingsManager,
        private val walletManager: IWalletManager
) : BlockchainSettingsModule.IInteractor {

    override fun coinWithSetting(coinType: CoinType): Coin? {
        return blockchainSettingsManager.coinsWithSettings.firstOrNull { it.type == coinType }
    }

    override fun blockchainSettings(coinType: CoinType): BlockchainSetting? {
        return blockchainSettingsManager.blockchainSettings(coinType)
    }

    override fun updateSettings(blockchainSettings: BlockchainSetting) {
        blockchainSettingsManager.updateSettings(blockchainSettings)
    }

    override fun getWalletForUpdate(coinType: CoinType): Wallet? {
        return walletManager.wallets.firstOrNull { it.coin.type == coinType }
    }

    override fun reSyncWallet(wallet: Wallet) {
        walletManager.delete(listOf(wallet))

        //start wallet with updated settings
        walletManager.save(listOf(wallet))
    }

}
