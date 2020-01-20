package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.IWalletFactory
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.*

class WalletStorage(
        private val appConfigProvider: IAppConfigProvider,
        private val walletFactory: IWalletFactory,
        private val storage: IEnabledWalletStorage)
    : IWalletStorage {

    override fun wallets(accounts: List<Account>): List<Wallet> {
        val coins = appConfigProvider.coins

        val enabledWallets = storage.enabledWallets
        return enabledWallets.map { enabledWallet ->
            val coin = coins.find { it.coinId == enabledWallet.coinId } ?: return@map null
            val account = accounts.find { it.id == enabledWallet.accountId } ?: return@map null
            walletFromEnabledWallet(enabledWallet, coin, account)
        }.mapNotNull { it }
    }

    override fun wallet(account: Account, coin: Coin): Wallet? {
        val enabledWallets = storage.enabledWallets
        enabledWallets.firstOrNull { it.coinId == coin.coinId && it.accountId == account.id }?.let { enabledWallet ->
            return walletFromEnabledWallet(enabledWallet, coin, account)
        }
        return null
    }

    private fun walletFromEnabledWallet(enabledWallet: EnabledWallet, coin: Coin, account: Account): Wallet {
        val coinSetting = mutableMapOf<CoinSetting, String>()
        enabledWallet.derivation?.let {
            coinSetting[CoinSetting.Derivation] = it
        }
        enabledWallet.syncMode?.let {
            coinSetting[CoinSetting.SyncMode] = it
        }
        return walletFactory.wallet(coin, account, coinSetting)
    }

    override fun enabledCoins(): List<Coin> {
        val coins = appConfigProvider.coins

        return storage.enabledWallets.map { enabledWallet ->
            val coin = coins.find { it.coinId == enabledWallet.coinId }
            coin
        }.mapNotNull { it }
    }

    override fun save(wallets: List<Wallet>) {
        val enabledWallets = mutableListOf<EnabledWallet>()

        wallets.forEachIndexed { index, wallet ->

            enabledWallets.add(
                    EnabledWallet(
                            wallet.coin.coinId,
                            wallet.account.id,
                            index,
                            wallet.settings[CoinSetting.SyncMode],
                            wallet.settings[CoinSetting.Derivation]
                    )
            )
        }

        storage.save(enabledWallets)
    }

    override fun delete(wallets: List<Wallet>) {
        val enabledWallets = wallets.map { EnabledWallet(it.coin.coinId, it.account.id) }
        storage.delete(enabledWallets)
    }
}
