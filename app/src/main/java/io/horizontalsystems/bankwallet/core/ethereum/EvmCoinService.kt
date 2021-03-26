package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.ICurrencyManager
import java.math.BigDecimal
import java.math.BigInteger

class EvmCoinService(
        val coin: Coin,
        private val currencyManager: ICurrencyManager,
        private val xRateManager: IRateManager
) : Clearable {

    val rate: CurrencyValue?
        get() {
            val baseCurrency = currencyManager.baseCurrency

            return xRateManager.latestRate(coin.type, baseCurrency.code)?.let {
                CurrencyValue(baseCurrency, it.rate)
            }
        }

    fun amountData(value: BigInteger): SendModule.AmountData {
        val decimalValue = BigDecimal(value, coin.decimal)
        val coinValue = CoinValue(coin, decimalValue)

        val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        val secondaryAmountInfo = rate?.let {
            SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(it.currency, it.value * decimalValue))
        }

        return SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
    }

    fun amountData(value: BigDecimal): SendModule.AmountData {
        return amountData(value.movePointRight(coin.decimal).toBigInteger())
    }

    fun coinValue(value: BigInteger): CoinValue {
        return CoinValue(coin, convertToMonetaryValue(value))
    }

    fun convertToMonetaryValue(value: BigInteger): BigDecimal {
        return value.toBigDecimal().movePointLeft(coin.decimal).stripTrailingZeros()
    }

    fun convertToFractionalMonetaryValue(value: BigDecimal): BigInteger {
        return value.movePointRight(coin.decimal).toBigInteger()
    }

    override fun clear() = Unit
}
