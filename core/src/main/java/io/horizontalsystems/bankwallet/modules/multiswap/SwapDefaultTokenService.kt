package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.core.defaultTokenQuery
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

class SwapDefaultTokenService(
    private val marketKit: MarketKitWrapper,
    private val walletManager: WalletManager
) : ServiceState<SwapDefaultTokenState>() {
    private var tokenOut: Token? = null

    override fun createState() = SwapDefaultTokenState(
        tokenOut = tokenOut
    )

    fun setTokenIn(token: Token) {
        determineTokenOut(token)

        emitState()
    }

    private fun determineTokenOut(token: Token) {
        // The default counterpart is the first entry of the context-aware Popular Tokens list,
        // built with the just-selected token as context — so the auto-pick always matches the
        // top bubble the user would see in the token picker for that token. This holds for every
        // token type: native context → its USDT (fallback USDT-ETH), non-native context → the
        // chain's native coin. See token_picker spec (Popular Tokens, Cases А/Б).
        tokenOut = SwapPopularTokens.build(marketKit, token).firstOrNull()
            ?: walletManager.activeWallets
                .firstOrNull { it.token.blockchainType == BlockchainType.Bitcoin }
                ?.token
            ?: marketKit.token(BlockchainType.Bitcoin.defaultTokenQuery)
    }
}

data class SwapDefaultTokenState(val tokenOut: Token?)
