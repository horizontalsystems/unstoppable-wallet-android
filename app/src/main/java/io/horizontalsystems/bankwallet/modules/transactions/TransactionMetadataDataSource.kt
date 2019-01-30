package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.math.BigDecimal

class TransactionMetadataDataSource {

    private val lastBlockHeights = mutableMapOf<CoinCode, Int>()
    private val thresholds = mutableMapOf<CoinCode, Int>()
    private val rates = mutableMapOf<CoinCode, MutableMap<Long, CurrencyValue>>()

    fun setLastBlockHeight(lastBlockHeight: Int, coinCode: CoinCode) {
        lastBlockHeights[coinCode] = lastBlockHeight
    }

    fun getLastBlockHeight(coinCode: CoinCode): Int? {
        return lastBlockHeights[coinCode]
    }

    fun setConfirmationThreshold(confirmationThreshold: Int, coinCode: CoinCode) {
        thresholds[coinCode] = confirmationThreshold
    }

    fun getConfirmationThreshold(coinCode: CoinCode): Int? =
            thresholds[coinCode]

    fun setRate(rateValue: BigDecimal, coinCode: CoinCode, currency: Currency, timestamp: Long) {
        if (!rates.containsKey(coinCode)) {
            rates[coinCode] = mutableMapOf()
        }

        rates[coinCode]?.set(timestamp, CurrencyValue(currency, rateValue))
    }

    fun getRate(coinCode: String, timestamp: Long): CurrencyValue? =
            rates[coinCode]?.get(timestamp)

    fun clearRates() {
        rates.clear()
    }

}
