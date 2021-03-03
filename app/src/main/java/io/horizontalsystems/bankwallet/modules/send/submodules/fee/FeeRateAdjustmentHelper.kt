package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountInfo
import io.horizontalsystems.coinkit.models.CoinType
import java.math.BigDecimal

class FeeRateAdjustmentHelper(private val appConfigProvider: IAppConfigProvider) {

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

    fun applyRule(coinType: CoinType, feeRateAdjustmentInfo: FeeRateAdjustmentInfo, feeRate: Long): Long {

        val coinRules = rulesByCoin[coinType] ?: return feeRate  //Binance, BCH, Dash, Litecoin has static fee

        val fallbackRate = (feeRate * 1.10F).toLong()

        if (!appConfigProvider.feeRateAdjustForCurrencies.contains(feeRateAdjustmentInfo.currency.code)){
            return fallbackRate
        }

        val resolvedCoinAmount: BigDecimal? = when (val amountInfo = feeRateAdjustmentInfo.amountInfo) {
            SendAmountInfo.Max -> feeRateAdjustmentInfo.balance
            is SendAmountInfo.Entered -> amountInfo.amount
            SendAmountInfo.NotEntered -> feeRateAdjustmentInfo.balance
        }

        val xRate = feeRateAdjustmentInfo.xRate ?: return fallbackRate

        val coinAmount = resolvedCoinAmount ?: return fallbackRate

        val fiatAmount = coinAmount * xRate

        var coefficient = 1.10F

        coinRules.firstOrNull { it.range.contains(fiatAmount.toLong()) }?.let {
            coefficient = it.coefficient
        }

        return (feeRate * coefficient).toLong()
    }

    inner class Rule(val range: LongRange, val coefficient: Float)
}
