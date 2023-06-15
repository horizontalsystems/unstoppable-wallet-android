package io.horizontalsystems.bankwallet.modules.balance.cex

import com.binance.connector.client.impl.SpotClientImpl
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.core.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal


class BinanceBalanceCexRepository(
    private val apiKey: String,
    private val secretKey: String
) : IBalanceCexRepository {
    override val itemsFlow = MutableStateFlow<List<BalanceCexItem>?>(null)

    private val coinMapper = BinanceCexCoinMapper(App.marketKit)

    private val gson = Gson()

    override fun start() {
        val client = SpotClientImpl(apiKey, secretKey)
        val trade = client.createTrade()
        try {
            val account = gson.fromJson(trade.account(mutableMapOf()), AccountInfo::class.java)

            itemsFlow.update {
                account
                    .balances
                    .filter { it.free > BigDecimal.ZERO }
                    .map {
                        BalanceCexItem(
                            balance = it.free,
                            coin = coinMapper.getCoin(it.asset),
                            decimals = 8
                        )
                    }
            }
        } catch (e: Exception) {

        }
    }

    override fun stop() {

    }

    data class AccountInfo(
        val balances: List<Balance>
    ) {
        data class Balance(
            val asset: String,
            val free: BigDecimal,
            val locked: BigDecimal,
        )
    }
    data class CoinInfo(
        val coin: String,
        val name: String,
        val free: BigDecimal,
        val freeze: BigDecimal,
    )
}