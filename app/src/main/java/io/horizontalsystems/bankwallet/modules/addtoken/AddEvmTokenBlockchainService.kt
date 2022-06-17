package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.CustomCoin
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.CoinType

class AddEvmTokenBlockchainService(
    private val blockchain: EvmBlockchain,
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

    override fun coinType(reference: String): CoinType {
        return blockchain.getEvm20CoinType(reference.lowercase())
    }

    override suspend fun customCoin(reference: String): CustomCoin {
        val tokenInfo = networkManager.getEvmTokeInfo(apiPath(blockchain), reference)
        return CustomCoin(coinType(reference), tokenInfo.name, tokenInfo.symbol, tokenInfo.decimals)
    }

    private fun apiPath(blockchain: EvmBlockchain): String = when (blockchain) {
        EvmBlockchain.ArbitrumOne -> "arbitrum-one"
        EvmBlockchain.BinanceSmartChain -> "bep20"
        EvmBlockchain.Ethereum -> "erc20"
        EvmBlockchain.Optimism -> "optimism"
        EvmBlockchain.Polygon -> "mrc20"
    }

}
