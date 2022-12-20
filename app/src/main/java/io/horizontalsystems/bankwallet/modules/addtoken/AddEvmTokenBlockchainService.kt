package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.*

class AddEvmTokenBlockchainService(
    private val blockchain: Blockchain,
    private val networkManager: INetworkManager
) : IAddTokenBlockchainService {

    override fun isValid(reference: String): Boolean {
        return try {
            AddressValidator.validate(reference)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun tokenQuery(reference: String): TokenQuery {
        return TokenQuery(blockchain.type, TokenType.Eip20(reference.lowercase()))
    }

    override suspend fun token(reference: String): Token {
        val tokenInfo = networkManager.getEvmTokeInfo(blockchain.type.uid, reference)
        val tokenQuery = tokenQuery(reference)
        return Token(
            coin = Coin(tokenQuery.customCoinUid, tokenInfo.name, tokenInfo.symbol, tokenInfo.decimals),
            blockchain = blockchain,
            type = tokenQuery.tokenType,
            decimals = tokenInfo.decimals
        )
    }
}
