package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.CustomCoin
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class AddBep2TokenBlockchainService(
    private val blockchainType: BlockchainType,
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

    override suspend fun customCoin(reference: String): CustomCoin {
        val tokenInfo = networkManager.getBep2TokeInfo(blockchainType.uid, reference)
        return CustomCoin(TokenQuery(BlockchainType.BinanceChain, TokenType.Bep2(reference)), tokenInfo.name, tokenInfo.originalSymbol, tokenInfo.decimals)
    }

}
