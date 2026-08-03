package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.ServiceState
import io.horizontalsystems.walletkit.core.defaultTokenQuery
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.core.supports
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

class SwapDefaultTokenService(
    private val marketKit: MarketKitWrapper,
    private val walletManager: WalletManager,
    private val accountManager: IAccountManager,
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
        // Entries the active account can never hold are skipped (e.g. everything but XMR for a
        // Monero-only account): the auto-pick must not name a coin the token picker itself hides.
        // The Bitcoin fallbacks must never yield the input token itself (e.g. a BTC-only
        // watch account, where the only active wallet IS the input token); the popular list
        // already excludes the context token by construction.
        tokenOut = SwapPopularTokens.build(marketKit, token).firstOrNull { supportedByAccount(it) }
            ?: walletManager.activeWallets
                .firstOrNull { it.token != token && it.token.blockchainType == BlockchainType.Bitcoin }
                ?.token
            ?: marketKit.token(BlockchainType.Bitcoin.defaultTokenQuery)
                ?.takeIf { it != token && supportedByAccount(it) }
    }

    private fun supportedByAccount(token: Token): Boolean {
        val accountType = accountManager.activeAccount?.type ?: return true
        return token.supports(accountType) && token.blockchainType.supports(accountType)
    }
}

data class SwapDefaultTokenState(val tokenOut: Token?)
