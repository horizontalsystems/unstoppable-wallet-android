package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.core.ICurrencyManager
import java.math.BigDecimal
import java.math.BigInteger

class EthereumCoinService(
        appConfigProvider: IAppConfigProvider,
        private val currencyManager: ICurrencyManager,
        private val xRateManager: IRateManager
) {

    val ethereumCoin: Coin = appConfigProvider.ethereumCoin

    val ethereumRate: CurrencyValue?
        get() {
            val baseCurrency = currencyManager.baseCurrency

            return xRateManager.marketInfo(ethereumCoin.code, baseCurrency.code)?.let {
                CurrencyValue(baseCurrency, it.rate)
            }
        }

    fun amountData(value: BigInteger): SendModule.AmountData {
        val primaryAmountInfo: SendModule.AmountInfo
        val secondaryAmountInfo: SendModule.AmountInfo?

        val decimalValue = BigDecimal(value, ethereumCoin.decimal)
        val coinValue = CoinValue(ethereumCoin, decimalValue)

        val rate = ethereumRate
        if (rate != null) {
            primaryAmountInfo = SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(rate.currency, rate.value * decimalValue))
            secondaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        } else {
            primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
            secondaryAmountInfo = null
        }

        return SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
    }

}
