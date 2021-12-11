package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.IAddTokenBlockchainService
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Single

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

    override fun coinType(reference: String): CoinType {
        val address = reference.lowercase()
        return when (blockchain) {
            Blockchain.Ethereum -> CoinType.Erc20(address)
            Blockchain.BinanceSmartChain -> CoinType.Bep20(address)
        }
    }

    override fun customTokenAsync(reference: String): Single<CustomToken> {
        return networkManager.getEvmTokeInfo(blockchain.tokenType, reference)
            .map { tokenInfo ->
                CustomToken(tokenInfo.name, tokenInfo.symbol, coinType(reference), tokenInfo.decimals)
            }
    }

    enum class Blockchain(val tokenType: String) {
        Ethereum("erc20"), BinanceSmartChain("bep20");
    }

}
