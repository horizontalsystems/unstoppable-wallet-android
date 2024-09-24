package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class AddBep2TokenBlockchainService(
    private val blockchain: Blockchain,
    private val networkManager: INetworkManager
) : IAddTokenBlockchainService {

    override fun isValid(reference: String): Boolean {
        //check reference for period in the middle
        val regex = "\\w+-\\w+".toRegex()
        return regex.matches(reference)
    }

    override fun tokenQuery(reference: String): TokenQuery {
        return TokenQuery(BlockchainType.BinanceChain, TokenType.Bep2(reference))
    }

    override suspend fun token(reference: String): Token {
        val bep2Tokens = networkManager.getBep2Tokens()
        val tokenInfo = bep2Tokens.firstOrNull { it.symbol == reference }
            ?: throw AddTokenService.TokenError.NotFound
        val tokenQuery = tokenQuery(reference)
        return Token(
            coin = Coin(
                uid = tokenQuery.customCoinUid,
                name = tokenInfo.name,
                code = tokenInfo.original_symbol
            ),
            blockchain = blockchain,
            type = tokenQuery.tokenType,
            decimals = 0
        )
    }
}
