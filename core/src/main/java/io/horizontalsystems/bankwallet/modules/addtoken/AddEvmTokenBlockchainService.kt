package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.erc20kit.core.Eip20Provider
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.rx2.await

class AddEvmTokenBlockchainService(
    private val blockchain: Blockchain,
    private val eip20Provider: Eip20Provider
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
        val tokenInfo = eip20Provider.getTokenInfo(Address(reference)).await()
        val tokenQuery = tokenQuery(reference)
        return Token(
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
        fun getInstance(blockchain: Blockchain): AddEvmTokenBlockchainService {
            val httpSyncSource = App.evmSyncSourceManager.getHttpSyncSource(blockchain.type)
            val httpRpcSource = httpSyncSource?.rpcSource as? RpcSource.Http
                ?: throw IllegalStateException("No HTTP RPC Source for blockchain ${blockchain.type}")

            val eip20Provider = Eip20Provider.instance(httpRpcSource)
            return AddEvmTokenBlockchainService(blockchain, eip20Provider)
        }
    }
}
