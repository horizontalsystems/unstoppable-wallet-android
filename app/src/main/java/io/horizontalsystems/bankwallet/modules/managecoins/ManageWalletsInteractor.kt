package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation

class ManageWalletsInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager,
        private val accountCreator: IAccountCreator,
        private val blockchainSettingsManager: IBlockchainSettingsManager
) : ManageWalletsModule.IInteractor {

    override val coins: List<Coin>
        get() = appConfigProvider.coins

    override val featuredCoins: List<Coin>
        get() = appConfigProvider.featuredCoins

    override val wallets: List<Wallet>
        get() = walletManager.wallets

    override val accounts: List<Account>
        get() = accountManager.accounts

    override fun loadAccounts() {
        accountManager.loadAccounts()
    }

    override fun loadWallets() {
        walletManager.loadWallets()
    }

    override fun save(wallet: Wallet) {
        walletManager.save(listOf(wallet))
    }

    override fun save(account: Account) {
        accountManager.save(account)
    }

    override fun delete(wallet: Wallet) {
        walletManager.delete(listOf(wallet))
    }

    override fun createAccount(predefinedAccountType: PredefinedAccountType): Account {
        return accountCreator.newAccount(predefinedAccountType)
    }

    override fun derivation(coinType: CoinType, forCreate: Boolean): Derivation? {
        return blockchainSettingsManager.derivationSetting(coinType, forCreate)?.derivation
    }

    override fun syncMode(coinType: CoinType, forCreate: Boolean): SyncMode? {
        return blockchainSettingsManager.syncModeSetting(coinType, forCreate)?.syncMode
    }

    override fun saveDerivation(coinType: CoinType, derivation: Derivation) {
        blockchainSettingsManager.updateSetting(DerivationSetting(coinType, derivation))
    }

    override fun saveSyncMode(coinType: CoinType, syncMode: SyncMode) {
        blockchainSettingsManager.updateSetting(SyncModeSetting(coinType, syncMode))
    }
}
