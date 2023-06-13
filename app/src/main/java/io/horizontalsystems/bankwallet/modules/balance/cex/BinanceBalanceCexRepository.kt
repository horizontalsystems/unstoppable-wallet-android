package cash.p.terminal.modules.balance.cex

import com.binance.connector.client.impl.SpotClientImpl
import com.google.gson.Gson
import cash.p.terminal.core.customCoinPrefix
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal


class BinanceBalanceCexRepository() : IBalanceCexRepository {
    override val itemsFlow = MutableStateFlow<List<BalanceCexItem>?>(null)

    private val apiKey = ""
    private val secretKey = ""
    private val gson = Gson()

    override fun start() {
        val client = SpotClientImpl(apiKey, secretKey)
        val wallet = client.createWallet()

        val coinInfo = gson.fromJson(wallet.coinInfo(mutableMapOf()), Array<CoinInfo>::class.java)

        val balance = coinInfo.filter { it.free > BigDecimal.ZERO }

        itemsFlow.update {
            balance.map {
                BalanceCexItem(
                    balance = it.free,
                    coin = Coin(
                        uid = "${TokenQuery.customCoinPrefix}${it.coin}",
                        name = it.name,
                        code = it.coin
                    ),
                    decimals = 8
                )
            }
        }
    }

    override fun stop() {

    }

    data class CoinInfo(
        val coin: String,
        val name: String,
        val free: BigDecimal,
        val freeze: BigDecimal,
    )
}