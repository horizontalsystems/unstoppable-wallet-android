package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.IAddTokenBlockchainService
import io.horizontalsystems.bankwallet.entities.ApiError
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.core.api.BinanceChainApi
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Single

class AddBep2TokenBlockchainService(
        appConfigProvider: IBuildConfigProvider
) : IAddTokenBlockchainService {

    private val networkType = if (appConfigProvider.testMode)
        BinanceChainKit.NetworkType.TestNet else
        BinanceChainKit.NetworkType.MainNet

    private val binanceApi = BinanceChainApi(networkType)

    override fun isValid(reference: String): Boolean {
        //check reference for period in the middle
        val regex = "\\w+-\\w+".toRegex()
        return regex.matches(reference)
    }

    override fun coinType(reference: String): CoinType {
        return CoinType.Bep2(reference)
    }

    override fun customTokenAsync(reference: String): Single<CustomToken> {
        return binanceApi.getTokens()
            .flatMap { tokens ->
                val token = tokens.firstOrNull { it.symbol.equals(reference, ignoreCase = true) }
                if (token != null) {
                    val customToken = CustomToken(
                        token.name,
                        token.code,
                        CoinType.Bep2(token.symbol),
                        8
                    )

                    Single.just(customToken)
                } else {
                    Single.error(ApiError.Bep2SymbolNotFound)
                }
            }
    }

}
