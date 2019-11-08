package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.IWalletFactory
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.bankwallet.entities.Wallet

class WalletStorage(
        private val appConfigProvider: IAppConfigProvider,
        private val walletFactory: IWalletFactory,
        private val storage: IEnabledWalletStorage)
    : IWalletStorage {

    override fun wallets(accounts: List<Account>): List<Wallet> {
        val coins = appConfigProvider.coins

        return storage.enabledWallets.map { enabledWallet ->
            val coin = coins.find { it.coinId == enabledWallet.coinId }
            val account = accounts.find { it.id == enabledWallet.accountId }

            if (coin != null && account != null) {
                walletFactory.wallet(coin, account, enabledWallet.syncMode)
            } else {
                null
            }
        }.mapNotNull { it }
    }

    override fun enabledCoins(): List<Coin> {
        val coins = appConfigProvider.coins

        return storage.enabledWallets.map {enabledWallet ->
            val coin = coins.find { it.coinId == enabledWallet.coinId }
            coin
        }.mapNotNull { it }
    }

    override fun save(wallets: List<Wallet>) {
        val enabledWallets = mutableListOf<EnabledWallet>()

        wallets.forEachIndexed { index, wallet ->
            enabledWallets.add(EnabledWallet(wallet.coin.coinId, wallet.account.id, index, wallet.syncMode))
        }

        storage.save(enabledWallets)
    }
}
