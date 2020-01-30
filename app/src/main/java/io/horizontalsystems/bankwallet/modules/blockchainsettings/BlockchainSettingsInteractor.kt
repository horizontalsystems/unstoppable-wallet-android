package io.horizontalsystems.bankwallet.modules.blockchainsettings

import io.horizontalsystems.bankwallet.core.IAccountCleaner
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ICoinSettingsManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Wallet

class BlockchainSettingsInteractor(
        private val coinSettingsManager: ICoinSettingsManager,
        private val walletManager: IWalletManager,
        private val accountCleaner: IAccountCleaner,
        private val appConfigProvider: IAppConfigProvider
) : CoinSettingsModule.IInteractor {

    override fun bitcoinDerivation(): AccountType.Derivation {
        return coinSettingsManager.bitcoinDerivation
    }

    override fun syncMode(): SyncMode {
        return coinSettingsManager.syncMode
    }

    override fun updateBitcoinDerivation(derivation: AccountType.Derivation) {
        coinSettingsManager.bitcoinDerivation = derivation
    }

    override fun updateSyncMode(source: SyncMode) {
        coinSettingsManager.syncMode = source
    }

    override fun getWalletsForSyncModeUpdate(): List<Wallet> {
        val enabledWallets = mutableListOf<Wallet>()
        appConfigProvider.coins.firstOrNull { it.code == "BTC" }?.let { coin ->
            walletManager.wallet(coin)?.let { wallet ->
                enabledWallets.add(wallet)
            }
        }
        appConfigProvider.coins.firstOrNull { it.code == "BCH" }?.let { coin ->
            walletManager.wallet(coin)?.let { wallet ->
                enabledWallets.add(wallet)
            }
        }
        appConfigProvider.coins.firstOrNull { it.code == "DASH" }?.let { coin ->
            walletManager.wallet(coin)?.let { wallet ->
                enabledWallets.add(wallet)
            }
        }
        return enabledWallets
    }

    override fun getWalletsForDerivationUpdate(): List<Wallet> {
        val enabledWallets = mutableListOf<Wallet>()
        appConfigProvider.coins.firstOrNull { it.code == "BTC" }?.let { coin ->
            walletManager.wallet(coin)?.let { wallet ->
                enabledWallets.add(wallet)
            }
        }
        return enabledWallets
    }

    override fun reSyncWalletsWithNewSettings(wallets: List<Wallet>) {
        //stop wallets
        walletManager.delete(wallets)

        //clear kits
        wallets.forEach {wallet ->
            accountCleaner.clearAccount(wallet.coin.type, wallet.account.id)
        }

        //start wallets with updated settings
        walletManager.save(wallets)
    }

}
