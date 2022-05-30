package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.Platform
import io.horizontalsystems.marketkit.models.PlatformCoin

class WalletStorage(
    private val marketKit: MarketKit,
    private val storage: IEnabledWalletStorage
) : IWalletStorage {

    override fun wallets(account: Account): List<Wallet> {
        val enabledWallets = storage.enabledWallets(account.id)

        val coinTypeIds = enabledWallets.map { it.coinId }
        val platformCoins = marketKit.platformCoinsByCoinTypeIds(coinTypeIds)

        return enabledWallets.mapNotNull { enabledWallet ->
            val coinSettings = CoinSettings(enabledWallet.coinSettingsId)

            platformCoins.find { it.coinType.id == enabledWallet.coinId }?.let { platformCoin ->
                val configuredPlatformCoin = ConfiguredPlatformCoin(platformCoin, coinSettings)
                return@mapNotNull Wallet(configuredPlatformCoin, account)
            }

            if (enabledWallet.coinName != null && enabledWallet.coinCode != null && enabledWallet.coinDecimals != null) {
                val coinType = CoinType.fromId(enabledWallet.coinId)
                val coinUid = coinType.customCoinUid
                val platformCoin = PlatformCoin(
                    platform = Platform(coinType, enabledWallet.coinDecimals, coinUid),
                    coin = Coin(coinUid, enabledWallet.coinName, enabledWallet.coinCode)
                )

                val configuredPlatformCoin = ConfiguredPlatformCoin(platformCoin, coinSettings)

                Wallet(configuredPlatformCoin, account)
            } else {
                null
            }
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

    override fun isEnabled(accountId: String, coinId: String): Boolean {
        return storage.isEnabled(accountId, coinId)
    }

    override fun clear() {
        storage.deleteAll()
    }

    private fun enabledWallet(wallet: Wallet, index: Int? = null): EnabledWallet {
        return EnabledWallet(
            wallet.platform.coinType.id,
            wallet.coinSettings.id,
            wallet.account.id,
            index,
            wallet.coin.name,
            wallet.coin.code,
            wallet.platform.decimals
        )
    }
}
