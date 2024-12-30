package cash.p.terminal.core.ethereum

import cash.p.terminal.core.ICoinManager
import io.horizontalsystems.core.CurrencyManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import io.horizontalsystems.ethereumkit.models.Address
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType

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
