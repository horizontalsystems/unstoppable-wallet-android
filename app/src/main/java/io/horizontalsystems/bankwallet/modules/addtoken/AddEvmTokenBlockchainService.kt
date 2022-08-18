package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.CustomCoin
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class AddEvmTokenBlockchainService(
    private val blockchainType: BlockchainType,
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
        return TokenQuery(blockchainType, TokenType.Eip20(reference.lowercase()))
    }

    override suspend fun customCoin(reference: String): CustomCoin {
        val tokenInfo = networkManager.getEvmTokeInfo(blockchainType.uid, reference)
        return CustomCoin(tokenQuery(reference), tokenInfo.name, tokenInfo.symbol, tokenInfo.decimals)
    }
}
