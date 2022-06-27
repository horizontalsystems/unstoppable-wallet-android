package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.xxxkit.MarketKit
import io.horizontalsystems.xxxkit.models.Token
import io.horizontalsystems.xxxkit.models.TokenQuery

class CoinManager(
    private val marketKit: io.horizontalsystems.marketkit.MarketKit,
    private val xxxKit: MarketKit,
    private val walletManager: IWalletManager
) : ICoinManager {

    private fun customPlatformCoin(coinType: CoinType): PlatformCoin? {
        return walletManager.activeWallets.find { it.coinType == coinType }?.platformCoin
    }

    override fun getPlatformCoin(coinType: CoinType): PlatformCoin? {
        return marketKit.platformCoin(coinType) ?: customPlatformCoin(coinType)
    }

    override fun getToken(query: TokenQuery): Token? {
        return xxxKit.token(query)
    }
}
