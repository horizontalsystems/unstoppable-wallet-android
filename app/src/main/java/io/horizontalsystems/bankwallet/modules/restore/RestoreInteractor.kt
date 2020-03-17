package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*

class RestoreInteractor(
        private val accountCreator: IAccountCreator,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val blockchainSettingsManager: IBlockchainSettingsManager
) : RestoreModule.IInteractor {


    override fun createAccounts(accounts: List<Account>) {
        accounts.forEach {
            accountManager.save(it)
        }
    }

    override fun saveWallets(wallets: List<Wallet>) {
        walletManager.save(wallets)
    }

    override fun getBlockchainSettings(coinType: CoinType): BlockchainSetting? {
        return blockchainSettingsManager.blockchainSettings(coinType)
    }

    override fun saveBlockchainSettings(settings: BlockchainSetting) {
        blockchainSettingsManager.updateSettings(settings)
    }

    @Throws
    override fun account(accountType: AccountType): Account {
        return accountCreator.restoredAccount(accountType)
    }

    override fun create(account: Account) {
        accountManager.save(account)
    }

}
