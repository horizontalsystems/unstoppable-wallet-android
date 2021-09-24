package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.*

class WalletStorage(
        private val coinManager: ICoinManager,
        private val storage: IEnabledWalletStorage)
    : IWalletStorage {

    override fun wallets(account: Account): List<Wallet> {
        val enabledWallets = storage.enabledWallets(account.id)

        val coinTypeIds = enabledWallets.map { it.coinId }
        val platformCoins = coinManager.getPlatformCoinsByCoinTypeIds(coinTypeIds)

        return enabledWallets.mapNotNull { enabledWallet ->
            val platformCoin = platformCoins.find { it.coinType.id == enabledWallet.coinId } ?: return@mapNotNull null

            val coinSettings = CoinSettings(enabledWallet.coinSettingsId)
            val configuredPlatformCoin = ConfiguredPlatformCoin(platformCoin, coinSettings)
            Wallet(configuredPlatformCoin, account)
        }
    }

    override fun save(wallets: List<Wallet>) {
        val enabledWallets = mutableListOf<EnabledWallet>()

        wallets.forEachIndexed { index, wallet ->

            enabledWallets.add(
                enabledWallet(wallet, index)
            )
        }

        storage.save(enabledWallets)
    }

    override fun delete(wallets: List<Wallet>) {
        storage.delete(wallets.map { enabledWallet(it) })
    }

    override fun clear() {
        storage.deleteAll()
    }

    private fun enabledWallet(wallet: Wallet, index: Int? = null): EnabledWallet {
        return EnabledWallet(
            wallet.platform.coinType.id,
            wallet.coinSettings.id,
            wallet.account.id,
            index
        )
    }
}
