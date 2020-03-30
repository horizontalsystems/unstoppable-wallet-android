package io.horizontalsystems.bankwallet.modules.blockchainsettings

import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation

class BlockchainSettingsInteractor(
        private val blockchainSettingsManager: IBlockchainSettingsManager,
        private val walletManager: IWalletManager
) : BlockchainSettingsModule.IInteractor {

    override fun coinWithSetting(coinType: CoinType): Coin? {
        return blockchainSettingsManager.coinsWithSettings.firstOrNull { it.type == coinType }
    }

    override fun derivation(coinType: CoinType): Derivation? {
        return blockchainSettingsManager.derivationSetting(coinType)?.derivation
    }

    override fun syncMode(coinType: CoinType): SyncMode? {
       return blockchainSettingsManager.syncModeSetting(coinType)?.syncMode
    }

    override fun saveDerivation(coinType: CoinType, derivation: Derivation) {
        blockchainSettingsManager.updateSetting(DerivationSetting(coinType, derivation))
    }

    override fun saveSyncMode(coinType: CoinType, derivation: SyncMode) {
        blockchainSettingsManager.updateSetting(SyncModeSetting(coinType, derivation))
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
