package cash.p.terminal.modules.balance.cex

import cash.p.terminal.core.App

class CoinzixBalanceCexRepository(authToken: String, secret: String) : IBalanceCexRepository {
    private val api = CoinzixCexApiService(authToken, secret)
    private val coinMapper = ConzixCexCoinMapper(App.marketKit)

    override suspend fun getItems(): List<BalanceCexItem> {
        return api.getBalances().map {
            val decimals = 8
            BalanceCexItem(
                balance = it.balance_available.movePointLeft(decimals),
                coin = coinMapper.getCoin(it.currency),
                decimals = decimals
            )
        }
    }
}
