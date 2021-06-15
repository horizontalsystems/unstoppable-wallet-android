package io.horizontalsystems.bankwallet.modules.addtoken.bep2

import io.horizontalsystems.bankwallet.core.InvalidBep2Address
import io.horizontalsystems.bankwallet.entities.ApiError
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.core.api.BinanceChainApi
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.IBuildConfigProvider
import io.reactivex.Single

class AddBep2TokenBlockchainService(
        appConfigProvider: IBuildConfigProvider
) {

    private val networkType = if (appConfigProvider.testMode)
        BinanceChainKit.NetworkType.TestNet else
        BinanceChainKit.NetworkType.MainNet

    private val binanceApi = BinanceChainApi(networkType)

    fun validate(reference: String) {
        //check reference for period in the middle
        val regex = "\\w+-\\w+".toRegex()
        if (!regex.matches(reference)){
            throw InvalidBep2Address()
        }
    }

    fun coinType(reference: String): CoinType {
        return CoinType.Bep2(reference)
    }

    fun coinAsync(reference: String) : Single<Coin>{
        return binanceApi.getTokens()
                .flatMap {tokens ->
                    val token = tokens.firstOrNull { it.symbol.equals(reference, ignoreCase = true) }
                    if (token != null){
                        val coin = Coin(
                                title = token.name,
                                code = token.code,
                                decimal = 8,
                                type = CoinType.Bep2(token.symbol)
                        )
                        Single.just(coin)
                    } else {
                        Single.error(ApiError.TokenNotFound)
                    }
                }
    }

}
