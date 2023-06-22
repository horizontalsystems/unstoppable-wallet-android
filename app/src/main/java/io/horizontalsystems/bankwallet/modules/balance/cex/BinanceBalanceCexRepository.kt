package io.horizontalsystems.bankwallet.modules.balance.cex

import com.binance.connector.client.impl.SpotClientImpl
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.core.App
import java.math.BigDecimal


class BinanceBalanceCexRepository(apiKey: String, secretKey: String) : IBalanceCexRepository {
    private val coinMapper = BinanceCexCoinMapper(App.marketKit)

    private val client = SpotClientImpl(apiKey, secretKey)
    private val trade = client.createTrade()
    private val gson = Gson()

    override suspend fun getItems(): List<BalanceCexItem> {
        val account = gson.fromJson(trade.account(mutableMapOf()), AccountInfo::class.java)
        return account
            .balances
            .filter { it.free > BigDecimal.ZERO }
            .map {
                BalanceCexItem(
                    balance = it.free,
                    coin = coinMapper.getCoin(it.asset),
                    decimals = 8,
                    assetId = it.asset
                )
            }
    }

    data class AccountInfo(val balances: List<Balance>) {
        data class Balance(
            val asset: String,
            val free: BigDecimal,
            val locked: BigDecimal,
        )
    }
}