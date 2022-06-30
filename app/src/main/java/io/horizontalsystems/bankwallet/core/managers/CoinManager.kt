package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.xxxkit.MarketKit
import io.horizontalsystems.xxxkit.models.Token
import io.horizontalsystems.xxxkit.models.TokenQuery

class CoinManager(
    private val marketKit: MarketKit,
    private val walletManager: IWalletManager
) : ICoinManager {

    override fun getToken(query: TokenQuery): Token? {
        return marketKit.token(query) ?: customToken(query)
    }

    private fun customToken(tokenQuery: TokenQuery): Token? {
        return walletManager.activeWallets.find { it.token.tokenQuery == tokenQuery }?.token
    }
}
