package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class EvmCoinServiceFactory(
    private val baseToken: Token,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val coinManager: ICoinManager
) {
    val baseCoinService = EvmCoinService(baseToken, currencyManager, marketKit)

    fun getCoinService(contractAddress: Address) = getCoinService(contractAddress.hex)

    fun getCoinService(contractAddress: String) = getToken(contractAddress)?.let { token ->
        EvmCoinService(token, currencyManager, marketKit)
    }

    fun getCoinService(token: Token) = EvmCoinService(token, currencyManager, marketKit)

    private fun getToken(contractAddress: String): Token? {
        val tokenQuery = TokenQuery(baseToken.blockchainType, TokenType.Eip20(contractAddress))
        return coinManager.getToken(tokenQuery)
    }

}
