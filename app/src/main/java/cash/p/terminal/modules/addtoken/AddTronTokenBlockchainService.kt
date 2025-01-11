package cash.p.terminal.modules.addtoken

import cash.p.terminal.core.App
import cash.p.terminal.wallet.customCoinUid
import cash.p.terminal.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.Blockchain
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.network.Network
import io.horizontalsystems.tronkit.rpc.Trc20Provider

class AddTronTokenBlockchainService(
    private val blockchain: Blockchain,
    private val trc20Provider: Trc20Provider
) : IAddTokenBlockchainService {

    override fun isValid(reference: String): Boolean {
        return try {
            Address.fromBase58(reference)
            true
        } catch (e: Throwable) {
            false
        }
    }

    override fun tokenQuery(reference: String): TokenQuery {
        return TokenQuery(blockchain.type, TokenType.Eip20(reference))
    }

    override suspend fun token(reference: String): Token {
        val tokenInfo = trc20Provider.getTokenInfo(Address.fromBase58(reference))
        val tokenQuery = tokenQuery(reference)
        return cash.p.terminal.wallet.Token(
            coin = Coin(
                uid = tokenQuery.customCoinUid,
                name = tokenInfo.tokenName,
                code = tokenInfo.tokenSymbol
            ),
            blockchain = blockchain,
            type = tokenQuery.tokenType,
            decimals = tokenInfo.tokenDecimal
        )
    }

    companion object {
        fun getInstance(blockchain: Blockchain): AddTronTokenBlockchainService {
            val trc20Provider = Trc20Provider.getInstance(Network.Mainnet, App.appConfigProvider.trongridApiKeys)
            return AddTronTokenBlockchainService(blockchain, trc20Provider)
        }
    }
}
