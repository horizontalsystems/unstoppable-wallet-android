package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin

class CoinManager(
    private val marketKit: MarketKit,
    private val walletManager: IWalletManager
) : ICoinManager {

    private fun customPlatformCoin(coinType: CoinType): PlatformCoin? {
        return walletManager.activeWallets.find { it.coinType == coinType }?.platformCoin
    }

    override fun getPlatformCoin(coinType: CoinType): PlatformCoin? {
        return marketKit.platformCoin(coinType) ?: customPlatformCoin(coinType)
    }
}
