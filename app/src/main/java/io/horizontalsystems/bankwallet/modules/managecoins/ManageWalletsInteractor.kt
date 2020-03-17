package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*

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

    override fun blockchainSettings(coinType: CoinType): BlockchainSetting? {
        return blockchainSettingsManager.blockchainSettings(coinType)
    }

    override fun blockchainSettingsForCreate(coinType: CoinType): BlockchainSetting? {
        return blockchainSettingsManager.blockchainSettingsForCreate(coinType)
    }

    override fun saveBlockchainSettings(blockchainSettings: BlockchainSetting) {
        blockchainSettingsManager.updateSettings(blockchainSettings)
    }
}
