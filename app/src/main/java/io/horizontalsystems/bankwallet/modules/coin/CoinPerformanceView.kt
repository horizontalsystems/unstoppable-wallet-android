package io.horizontalsystems.bankwallet.modules.coin

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.xrateskit.entities.TimePeriod
import kotlinx.android.synthetic.main.view_coin_performance.view.*
import java.math.BigDecimal

class CoinPerformanceView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.view_coin_performance, this)
    }

    fun bind(rateDiffs: Map<TimePeriod, Map<String, BigDecimal>>) {
        rateDiffs.forEach { (period, values) ->
            val usdValue = values["USD"] ?: BigDecimal.ZERO
            val ethValue = values["ETH"] ?: BigDecimal.ZERO
            val btcValue = values["BTC"] ?: BigDecimal.ZERO

            when (period) {
                TimePeriod.DAY_7 -> {
                    setColoredPercentageValue(weekUsd, usdValue)
                    setColoredPercentageValue(weekEth, ethValue)
                    setColoredPercentageValue(weekBtc, btcValue)
                }
                TimePeriod.DAY_30 -> {
                    setColoredPercentageValue(monthUsd, usdValue)
                    setColoredPercentageValue(monthEth, ethValue)
                    setColoredPercentageValue(monthBtc, btcValue)
                }
            }
        }
    }

    private fun setColoredPercentageValue(textView: TextView, percentage: BigDecimal) {
        val color = if (percentage >= BigDecimal.ZERO) R.color.remus else R.color.lucian
        val sign = if (percentage >= BigDecimal.ZERO) "+" else "-"

        textView.setTextColor(context.getColor(color))
        textView.text = App.numberFormatter.format(percentage.abs(), 0, 2, prefix = sign, suffix = "%")
    }

}
