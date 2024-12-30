package cash.p.terminal.modules.addtoken

import cash.p.terminal.wallet.customCoinUid
import cash.p.terminal.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.Blockchain
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
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
        return cash.p.terminal.wallet.Token(
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
