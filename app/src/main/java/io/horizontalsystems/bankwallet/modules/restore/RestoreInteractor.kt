package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.Wallet

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

    override fun initializeSettings(coinType: CoinType) {
        blockchainSettingsManager.initializeSettings(coinType)
    }

    @Throws
    override fun account(accountType: AccountType): Account {
        return accountCreator.restoredAccount(accountType)
    }

    override fun create(account: Account) {
        accountManager.save(account)
    }

}
