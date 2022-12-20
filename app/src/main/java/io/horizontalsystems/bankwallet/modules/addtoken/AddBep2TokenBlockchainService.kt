package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.*

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
        val tokenInfo = networkManager.getBep2TokeInfo(blockchain.type.uid, reference)
        val tokenQuery = tokenQuery(reference)
        return Token(
            coin = Coin(tokenQuery.customCoinUid, tokenInfo.name, tokenInfo.originalSymbol, tokenInfo.decimals),
            blockchain = blockchain,
            type = tokenQuery.tokenType,
            decimals = tokenInfo.decimals
        )
    }
}
