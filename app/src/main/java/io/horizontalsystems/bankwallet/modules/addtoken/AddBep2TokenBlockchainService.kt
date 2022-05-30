package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.CustomCoin
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Single

class AddBep2TokenBlockchainService(
    private val networkManager: INetworkManager
) : IAddTokenBlockchainService {

    override fun isValid(reference: String): Boolean {
        //check reference for period in the middle
        val regex = "\\w+-\\w+".toRegex()
        return regex.matches(reference)
    }

    override fun coinType(reference: String): CoinType {
        return CoinType.Bep2(reference)
    }

    override fun customCoinsSingle(reference: String): Single<CustomCoin> {
        return networkManager.getBep2TokeInfo(reference)
            .map { tokenInfo ->
                CustomCoin(CoinType.Bep2(reference), tokenInfo.name, tokenInfo.originalSymbol, tokenInfo.decimals)
            }
    }

}
