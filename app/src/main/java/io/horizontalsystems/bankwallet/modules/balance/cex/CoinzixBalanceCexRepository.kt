package cash.p.terminal.modules.balance.cex

import cash.p.terminal.core.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CoinzixBalanceCexRepository(authToken: String, secret: String) : IBalanceCexRepository {
    override val itemsFlow = MutableStateFlow<List<BalanceCexItem>?>(null)

    private val api = CoinzixCexApiService(authToken, secret)
    private val coinMapper = ConzixCexCoinMapper(App.marketKit)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun start() {
        coroutineScope.launch {
            itemsFlow.update {
                api.getBalances().map {
                    val decimals = 8
                    BalanceCexItem(
                        balance = it.balance_available.movePointLeft(decimals),
                        coin = coinMapper.getCoin(it.currency),
                        decimals = decimals
                    )
                }
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }
}
