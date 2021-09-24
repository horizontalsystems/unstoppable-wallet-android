package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType

class ActivateCoinManager(
        private val marketKit: MarketKit,
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager
) {

    fun activate(coinType: CoinType) {
        val platformCoin = marketKit.platformCoin(coinType) ?: return // coin type is not supported

        if (walletManager.activeWallets.any { it.platformCoin == platformCoin })  return // wallet already exists

        val account = accountManager.activeAccount ?: return // active account does not exist

        val wallet = Wallet(platformCoin, account)
        walletManager.save(listOf(wallet))
    }

}
