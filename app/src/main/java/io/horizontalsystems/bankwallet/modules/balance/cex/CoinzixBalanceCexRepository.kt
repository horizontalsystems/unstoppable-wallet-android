package cash.p.terminal.modules.balance.cex

import cash.p.terminal.core.App
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

class CoinzixBalanceCexRepository : IBalanceCexRepository {
    override val itemsFlow = MutableStateFlow(
        listOf(
            BalanceCexItem(
                balance = BigDecimal("123.43"),
                coin = App.marketKit.allCoins().first(),
                decimals = 8,
            ),
            BalanceCexItem(
                balance = BigDecimal("43.23343424"),
                coin = App.marketKit.allCoins().last(),
                decimals = 8,
            ),
        )
    )

    override fun start() {

    }

    override fun stop() {

    }
}
