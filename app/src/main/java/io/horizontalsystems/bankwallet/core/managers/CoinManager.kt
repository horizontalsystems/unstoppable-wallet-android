package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinManager @Inject constructor(
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
