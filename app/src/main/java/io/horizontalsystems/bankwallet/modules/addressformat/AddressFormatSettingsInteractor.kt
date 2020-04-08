package io.horizontalsystems.bankwallet.modules.addressformat

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.entities.Wallet

class AddressFormatSettingsInteractor(
        private val derivationSettingsManager: IDerivationSettingsManager,
        private val appConfigProvider: IAppConfigProvider,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager
) : AddressFormatSettingsModule.IInteractor {

    override fun derivation(coinType: CoinType): Derivation {
        return derivationSettingsManager.derivationSetting(coinType)?.derivation
                ?: derivationSettingsManager.defaultDerivationSetting(coinType)?.derivation
                ?: throw Exception("No derivation found for ${coinType.javaClass.simpleName}")
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
        adapterManager.refreshAdapters(listOf(wallet))
    }
}
