package cash.p.terminal.modules.addtoken

import cash.p.terminal.core.INetworkManager
import cash.p.terminal.wallet.customCoinUid
import cash.p.terminal.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType

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
        return cash.p.terminal.wallet.Token(
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
