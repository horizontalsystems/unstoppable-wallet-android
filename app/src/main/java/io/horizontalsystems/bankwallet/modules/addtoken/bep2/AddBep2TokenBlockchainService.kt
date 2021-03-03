package io.horizontalsystems.bankwallet.modules.addtoken.bep2

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.IAddTokenBlockchainService
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.ApiError
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.IBuildConfigProvider
import io.reactivex.Single

class AddBep2TokenBlockchainService(
        private val appConfigTestModer: IBuildConfigProvider,
        private val networkManager: INetworkManager
): IAddTokenBlockchainService {

    override fun validate(reference: String) {
        //Not yet implemented
    }

    override fun coinType(reference: String): CoinType {
        return CoinType.Bep2(reference)
    }

    override fun coinSingle(reference: String): Single<Coin> {
        val host = if (appConfigTestModer.testMode) "https://testnet-dex-atlantic.binance.org/api/v1/tokens/" else "https://dex.binance.org/api/v1/tokens/"
        val request = "?limit=10000"

        return networkManager.getBep2Tokens(host, request)
                .firstOrError()
                .flatMap {tokens ->
                    val token = tokens.firstOrNull { it.symbol.equals(reference, ignoreCase = true) }
                    if (token != null){
                        val coin = Coin(
                                title = token.name,
                                code = token.originalSymbol,
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

class Bep2Token(
        val name: String,
        @SerializedName("original_symbol")val originalSymbol: String,
        val symbol: String
)
