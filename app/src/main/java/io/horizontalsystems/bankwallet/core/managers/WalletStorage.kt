package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.coinkit.models.Coin

class WalletStorage(
        private val coinManager: ICoinManager,
        private val storage: IEnabledWalletStorage)
    : IWalletStorage {

    override fun wallets(accounts: List<Account>): List<Wallet> {
        val coins = coinManager.coins

        val enabledWallets = storage.enabledWallets
        return enabledWallets.map { enabledWallet ->
            val coin = coins.find { it.id == enabledWallet.coinId } ?: return@map null
            val account = accounts.find { it.id == enabledWallet.accountId } ?: return@map null
            Wallet(coin, account)
        }.mapNotNull { it }
    }

    override fun wallet(account: Account, coin: Coin): Wallet? {
        val enabledWallets = storage.enabledWallets
        enabledWallets.firstOrNull { it.coinId == coin.id && it.accountId == account.id }?.let { _ ->
            return Wallet(coin, account)
        }
        return null
    }

    override fun enabledCoins(): List<Coin> {
        val coins = coinManager.coins

        return storage.enabledWallets.map { enabledWallet ->
            val coin = coins.find { it.id == enabledWallet.coinId }
            coin
        }.mapNotNull { it }
    }

    override fun save(wallets: List<Wallet>) {
        val enabledWallets = mutableListOf<EnabledWallet>()

        wallets.forEachIndexed { index, wallet ->

            enabledWallets.add(
                    EnabledWallet(
                            wallet.coin.id,
                            wallet.account.id,
                            index
                    )
            )
        }

        storage.save(enabledWallets)
    }

    override fun delete(wallets: List<Wallet>) {
        val enabledWallets = wallets.map { EnabledWallet(it.coin.id, it.account.id) }
        storage.delete(enabledWallets)
    }
}
