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
            val coin = coins.find { it.coinId == enabledWallet.coinId }
            val account = accounts.find { it.id == enabledWallet.accountId }

            if (coin != null && account != null) {
                val coinSetting = mutableMapOf<CoinSetting, String>()
                enabledWallet.derivation?.let {
                    coinSetting[CoinSetting.Derivation] = it
                }
                enabledWallet.syncMode?.let {
                    coinSetting[CoinSetting.SyncMode] = it
                }
                walletFactory.wallet(coin, account, coinSetting)
            } else {
                null
            }
        }.mapNotNull { it }
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
