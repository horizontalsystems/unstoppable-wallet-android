package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.protocolType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

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
            is TokenType.Unsupported -> null
        }

        return tokenQuery?.let {
            marketKit.token(it)?.let { feeToken ->
                Pair(feeToken, feeToken.protocolType!!)
            }
        }
    }
}
