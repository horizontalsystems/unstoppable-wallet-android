package com.quantum.wallet.bankwallet.modules.multiswap

import com.quantum.wallet.bankwallet.core.ServiceState
import com.quantum.wallet.bankwallet.core.managers.MarketKitWrapper
import com.quantum.wallet.bankwallet.core.managers.WalletManager
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

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
        val blockchainType = token.blockchainType

        if (token.type == TokenType.Native) {
            val coinUid = when (blockchainType) {
                BlockchainType.BinanceSmartChain -> "binance-bridged-usdt-bnb-smart-chain"
                else -> "tether"
            }

            tokenOut = marketKit.fullCoins(listOf(coinUid))
                .firstOrNull()
                ?.let { fullCoin ->
                    fullCoin.tokens.firstOrNull { it.blockchainType == blockchainType }
                }

            if (tokenOut == null) {
                tokenOut = walletManager.activeWallets
                    .firstOrNull { it.token.blockchainType == BlockchainType.Bitcoin }
                    ?.token
                    ?: marketKit.token(TokenQuery(BlockchainType.Bitcoin, TokenType.Derived(TokenType.Derivation.Bip84)))
            }
        } else if (token.type is TokenType.Derived) {
            val targetBlockchainType = if (blockchainType == BlockchainType.Bitcoin) BlockchainType.Monero else BlockchainType.Bitcoin
            tokenOut = walletManager.activeWallets
                .firstOrNull { it.token.blockchainType == targetBlockchainType }
                ?.token
                ?: if (targetBlockchainType == BlockchainType.Bitcoin) {
                    marketKit.token(TokenQuery(BlockchainType.Bitcoin, TokenType.Derived(TokenType.Derivation.Bip84)))
                } else {
                    marketKit.token(TokenQuery(BlockchainType.Monero, TokenType.Native))
                }
        } else {
            tokenOut = marketKit.token(TokenQuery(blockchainType, TokenType.Native))
        }
    }
}

data class SwapDefaultTokenState(val tokenOut: Token?)
