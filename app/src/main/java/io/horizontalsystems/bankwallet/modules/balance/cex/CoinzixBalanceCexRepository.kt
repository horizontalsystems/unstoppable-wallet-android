package io.horizontalsystems.bankwallet.modules.balance.cex

import io.horizontalsystems.bankwallet.core.App
import java.math.BigDecimal

class CoinzixBalanceCexRepository(
    private val authToken: String,
    private val secret: String
) : IBalanceCexRepository {
    private val api = CoinzixCexApiService()
    private val coinMapper = ConzixCexCoinMapper(App.marketKit)

    override suspend fun getItems(): List<BalanceCexItem> {
        return api.getBalances(authToken, secret)
            .filter { it.balance_available > BigDecimal.ZERO }
            .map {
                val decimals = 8
                BalanceCexItem(
                    balance = it.balance_available.movePointLeft(decimals),
                    coin = coinMapper.getCoin(it.currency),
                    decimals = decimals,
                    assetId = it.currency.iso3
                )
            }
    }
}
