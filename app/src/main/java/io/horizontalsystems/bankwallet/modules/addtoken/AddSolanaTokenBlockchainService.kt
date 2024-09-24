package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.solanakit.core.TokenProvider
import io.horizontalsystems.solanakit.models.Address
import io.horizontalsystems.solanakit.transactions.SolanaFmService

class AddSolanaTokenBlockchainService(
    private val blockchain: Blockchain,
    private val tokenProvider: TokenProvider
) : IAddTokenBlockchainService {

    override fun isValid(reference: String): Boolean {
        return try {
            Address(reference)
            true
        } catch (e: Throwable) {
            false
        }
    }

    override fun tokenQuery(reference: String): TokenQuery {
        return TokenQuery(blockchain.type, TokenType.Spl(reference))
    }

    override suspend fun token(reference: String): Token {
        val tokenInfo = tokenProvider.getTokenInfo(reference)
        val tokenQuery = tokenQuery(reference)
        return Token(
            coin = Coin(
                uid = tokenQuery.customCoinUid,
                name = tokenInfo.name,
                code = tokenInfo.symbol
            ),
            blockchain = blockchain,
            type = tokenQuery.tokenType,
            decimals = tokenInfo.decimals
        )
    }

    companion object {
        fun getInstance(blockchain: Blockchain): AddSolanaTokenBlockchainService {
            val tokenProvider = TokenProvider(SolanaFmService())
            return AddSolanaTokenBlockchainService(blockchain, tokenProvider)
        }
    }

}
