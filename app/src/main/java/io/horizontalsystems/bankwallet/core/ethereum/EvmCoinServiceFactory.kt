package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.xxxkit.MarketKit
import io.horizontalsystems.xxxkit.models.Token
import io.horizontalsystems.xxxkit.models.TokenQuery
import io.horizontalsystems.xxxkit.models.TokenType

class EvmCoinServiceFactory(
    private val baseToken: Token,
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager
) {
    val baseCoinService = EvmCoinService(baseToken, currencyManager, marketKit)

    fun getCoinService(contractAddress: Address) = getCoinService(contractAddress.hex)

    fun getCoinService(contractAddress: String) = getToken(contractAddress)?.let { token ->
        EvmCoinService(token, currencyManager, marketKit)
    }

    fun getCoinService(token: Token) = EvmCoinService(token, currencyManager, marketKit)

    private fun getToken(contractAddress: String): Token? {
        val tokenQuery = TokenQuery(baseToken.blockchainType, TokenType.Eip20(contractAddress))
        return marketKit.token(tokenQuery)
    }

}
