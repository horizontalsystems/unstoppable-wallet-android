package cash.p.terminal.core.providers

import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.protocolType

class FeeTokenProvider(
    private val marketKit: MarketKitWrapper
) {

    fun feeTokenData(token: Token): Pair<Token, String>? {
        val tokenQuery = when (token.type) {
            is TokenType.Eip20,
            is TokenType.Spl,
            is TokenType.Bep2,
            is TokenType.Jetton,
            -> {
                TokenQuery(token.blockchainType, TokenType.Native)
            }
            TokenType.Native,
            is TokenType.Derived,
            is TokenType.AddressTyped,
            is TokenType.AddressSpecTyped,
            is TokenType.Unsupported -> null
        }

        return tokenQuery?.let {
            marketKit.token(it)?.let { feeToken ->
                Pair(feeToken, feeToken.protocolType!!)
            }
        }
    }
}
