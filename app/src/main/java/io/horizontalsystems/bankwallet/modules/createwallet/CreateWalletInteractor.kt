package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation

class CreateWalletInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private val accountCreator: IAccountCreator,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val blockchainSettingsManager: IBlockchainSettingsManager
) : CreateWalletModule.IInteractor {

    override val coins: List<Coin>
        get() = appConfigProvider.coins

    override val featuredCoins: List<Coin>
        get() = appConfigProvider.featuredCoins

    override fun createAccounts(accounts: List<Account>) {
        accounts.forEach {
            accountManager.save(it)
        }
    }

    override fun saveWallets(wallets: List<Wallet>) {
        walletManager.save(wallets)
    }

    @Throws
    override fun account(predefinedAccountType: PredefinedAccountType): Account {
        return accountCreator.newAccount(predefinedAccountType)
    }

    override fun derivation(coinType: CoinType): Derivation? {
        return blockchainSettingsManager.derivationSetting(coinType, true)?.derivation
    }

    override fun syncMode(coinType: CoinType): SyncMode? {
        return blockchainSettingsManager.syncModeSetting(coinType, true)?.syncMode
    }

    override fun saveDerivation(coinType: CoinType, derivation: Derivation) {
        blockchainSettingsManager.updateSetting(DerivationSetting(coinType, derivation))
    }

    override fun saveSyncMode(coinType: CoinType, syncMode: SyncMode) {
        blockchainSettingsManager.updateSetting(SyncModeSetting(coinType, syncMode))
    }

}
