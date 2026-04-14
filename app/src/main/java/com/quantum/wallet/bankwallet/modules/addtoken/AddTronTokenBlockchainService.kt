package com.quantum.wallet.bankwallet.modules.addtoken

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.customCoinUid
import com.quantum.wallet.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.models.RpcSource
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
        fun getInstance(blockchain: Blockchain): AddTronTokenBlockchainService {
            val trc20Provider = Trc20Provider.getInstance(RpcSource.tronGrid(Network.Mainnet, App.appConfigProvider.trongridApiKeys))
            return AddTronTokenBlockchainService(blockchain, trc20Provider)
        }
    }
}
