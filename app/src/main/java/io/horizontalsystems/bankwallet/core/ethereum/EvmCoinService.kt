package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal
import java.math.BigInteger

class EvmCoinService(
    val platformCoin: PlatformCoin,
    private val currencyManager: ICurrencyManager,
    private val marketKit: MarketKit
) : Clearable {

    val rate: CurrencyValue?
        get() {
            val baseCurrency = currencyManager.baseCurrency
            return marketKit.coinPrice(platformCoin.coin.uid, baseCurrency.code)?.let {
                CurrencyValue(baseCurrency, it.value)
            }
        }

    fun amountData(value: BigInteger): SendModule.AmountData {
        val decimalValue = BigDecimal(value, platformCoin.decimals)
        val coinValue = CoinValue(CoinValue.Kind.PlatformCoin(platformCoin), decimalValue)

        val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        val secondaryAmountInfo = rate?.let {
            SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(it.currency, it.value * decimalValue))
        }

        return SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
    }

    fun amountData(value: BigDecimal): SendModule.AmountData {
        return amountData(value.movePointRight(platformCoin.decimals).toBigInteger())
    }

    fun coinValue(value: BigInteger): CoinValue {
        return CoinValue(CoinValue.Kind.PlatformCoin(platformCoin), convertToMonetaryValue(value))
    }

    fun convertToMonetaryValue(value: BigInteger): BigDecimal {
        return value.toBigDecimal().movePointLeft(platformCoin.decimals).stripTrailingZeros()
    }

    fun convertToFractionalMonetaryValue(value: BigDecimal): BigInteger {
        return value.movePointRight(platformCoin.decimals).toBigInteger()
    }

    override fun clear() = Unit
}
