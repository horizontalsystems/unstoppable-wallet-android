package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.math.BigDecimal

class TransactionMetadataDataSource {

    private val lastBlockHeights = mutableMapOf<Coin, Int>()
    private val thresholds = mutableMapOf<Coin, Int>()
    private val rates = mutableMapOf<Coin, MutableMap<Long, CurrencyValue>>()

    fun setLastBlockHeight(lastBlockHeight: Int, coin: Coin) {
        lastBlockHeights[coin] = lastBlockHeight
    }

    fun getLastBlockHeight(coinCode: Coin): Int? {
        return lastBlockHeights[coinCode]
    }

    fun setConfirmationThreshold(confirmationThreshold: Int, coin: Coin) {
        thresholds[coin] = confirmationThreshold
    }

    fun getConfirmationThreshold(coin: Coin): Int? =
            thresholds[coin]

    fun setRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long) {
        if (!rates.containsKey(coin)) {
            rates[coin] = mutableMapOf()
        }

        rates[coin]?.set(timestamp, CurrencyValue(currency, rateValue))
    }

    fun getRate(coin: Coin, timestamp: Long): CurrencyValue? {
        return rates[coin]?.get(timestamp)
    }

    fun clearRates() {
        rates.clear()
    }

}
