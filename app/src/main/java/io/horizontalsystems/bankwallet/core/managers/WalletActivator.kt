package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.ConfiguredPlatformCoin
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.defaultSettingsArray
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType

class WalletActivator(
    private val walletManager: IWalletManager,
    private val marketKit: MarketKit,
    private val walletStorage: IWalletStorage,
) {

    fun activateWallets(account: Account, coinTypes: List<CoinType>) {
        val wallets = mutableListOf<Wallet>()

        for (coinType in coinTypes) {
            val platformCoin = marketKit.platformCoin(coinType) ?: continue

            val defaultSettingsArray = coinType.defaultSettingsArray

            if (defaultSettingsArray.isEmpty()) {
                wallets.add(Wallet(platformCoin, account))
            } else {
                defaultSettingsArray.forEach { coinSettings ->
                    val configuredPlatformCoin = ConfiguredPlatformCoin(platformCoin, coinSettings)
                    wallets.add(Wallet(configuredPlatformCoin, account))
                }
            }
        }

        walletManager.save(wallets)
    }

    fun isEnabled(account: Account, coinType: CoinType) = walletStorage.isEnabled(account.id, coinType.id)

}
