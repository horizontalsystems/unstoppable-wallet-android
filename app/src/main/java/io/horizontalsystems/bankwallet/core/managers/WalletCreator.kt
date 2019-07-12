package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Coin

class WalletCreator(private val accountManager: IAccountManager, private val walletFactory: IWalletFactory) : IWalletCreator {
    override fun wallet(coin: Coin): Wallet? {
        val suitableAccounts = accountManager.accounts.filter { account ->
            coin.type.canSupport(account.type)
        }

        val account = suitableAccounts.firstOrNull() ?: return null

        return walletFactory.wallet(coin, account)
    }

    override fun wallet(coin: Coin, account: Account): Wallet {
        return walletFactory.wallet(coin, account)
    }
}
