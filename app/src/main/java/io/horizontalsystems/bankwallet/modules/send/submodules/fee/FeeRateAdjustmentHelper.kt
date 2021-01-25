package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.CurrencyValue

class FeeRateAdjustmentHelper {

    private val amountRules = listOf(
            Rule(10001..Long.MAX_VALUE, 1.25f),
            Rule(5001..10000L, 1.20f),
            Rule(1001..5000L, 1.15f),
            Rule(501..1000L, 1.10f),
            Rule(0..500L, 1.05f)
    )

    private val rulesByCoin = mapOf(
            CoinType.Bitcoin to amountRules,
            CoinType.Ethereum to amountRules
    )

    fun applyRule(coinType: CoinType, currencyValue: CurrencyValue?, feeRate: Long): Long {

        val fallbackRate = (feeRate * 1.10F).toLong()

        currencyValue ?: return fallbackRate

        val coinRules = rulesByCoin[coinType] ?: return fallbackRate

        if (!currencyValue.currency.code.contains("USD")
                && !currencyValue.currency.code.contains("EUR")) {
            return fallbackRate
        }

        var coefficient = 1.10F

        coinRules.firstOrNull { it.range.contains(currencyValue.value.toLong()) }?.let {
            coefficient = it.coefficient
        }

        return (feeRate * coefficient).toLong()
    }

    inner class Rule(val range: LongRange, val coefficient: Float)
}
