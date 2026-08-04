package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.core.ServiceState
import io.horizontalsystems.walletkit.core.defaultTokenQuery
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

class SwapDefaultTokenService(
    private val marketKit: MarketKitWrapper,
    private val walletManager: WalletManager,
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
        // Tokens the account can't hold are legitimate picks: the "You Get" picker offers them
        // too, delivered to an external recipient address entered before confirmation.
        // The Bitcoin fallbacks must never yield the input token itself (e.g. a BTC-only
        // watch account, where the only active wallet IS the input token); the popular list
        // already excludes the context token by construction.
        tokenOut = SwapPopularTokens.build(marketKit, token).firstOrNull()
            ?: walletManager.activeWallets
                .firstOrNull { it.token != token && it.token.blockchainType == BlockchainType.Bitcoin }
                ?.token
            ?: marketKit.token(BlockchainType.Bitcoin.defaultTokenQuery)
                ?.takeIf { it != token }
    }
}

data class SwapDefaultTokenState(val tokenOut: Token?)
