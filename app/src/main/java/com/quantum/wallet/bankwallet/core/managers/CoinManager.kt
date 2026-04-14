package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.ICoinManager
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery

class CoinManager(
    private val marketKit: MarketKitWrapper,
    private val walletManager: WalletManager
) : ICoinManager {

    override fun getToken(query: TokenQuery): Token? {
        return marketKit.token(query) ?: customToken(query)
    }

    private fun customToken(tokenQuery: TokenQuery): Token? {
        return walletManager.activeWallets.find { it.token.tokenQuery == tokenQuery }?.token
    }
}
