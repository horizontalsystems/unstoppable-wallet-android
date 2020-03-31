package io.horizontalsystems.bankwallet.modules.bipsettings

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation

class BipSettingsInteractor(
        private val derivationSettingsManager: IDerivationSettingsManager,
        private val appConfigProvider: IAppConfigProvider,
        private val walletManager: IWalletManager
) : BipSettingsModule.IInteractor {

    override fun derivation(coinType: CoinType): Derivation {
        return derivationSettingsManager.derivationSetting(coinType)?.derivation
                ?: derivationSettingsManager.defaultDerivationSetting(coinType).derivation
    }

    override fun getCoin(coinType: CoinType): Coin {
        return appConfigProvider.coins.first { it.type == coinType }
    }

    override fun getWalletForUpdate(coinType: CoinType): Wallet? {
        return walletManager.wallets.firstOrNull { it.coin.type == coinType }
    }

    override fun saveDerivation(derivationSetting: DerivationSetting) {
        derivationSettingsManager.updateSetting(derivationSetting)
    }

    override fun reSyncWallet(wallet: Wallet) {
        walletManager.delete(listOf(wallet))

        //start wallet with updated settings
        walletManager.save(listOf(wallet))
    }
}
