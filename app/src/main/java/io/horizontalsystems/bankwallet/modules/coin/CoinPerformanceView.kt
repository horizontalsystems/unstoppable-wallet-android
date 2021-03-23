package io.horizontalsystems.bankwallet.modules.coin

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.xrateskit.entities.TimePeriod
import kotlinx.android.synthetic.main.view_coin_performance.view.*
import java.math.BigDecimal

class CoinPerformanceView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.view_coin_performance, this)
    }

    fun bind(currencyCode: String, rateDiffs: Map<TimePeriod, Map<String, BigDecimal>>) {
        currencyTitle.text = currencyCode
        rateDiffs.forEach { (period, values) ->
            val currencyValue = values[currencyCode] ?: BigDecimal.ZERO
            val ethValue = values["ETH"]
            val btcValue = values["BTC"]

            when (period) {
                TimePeriod.DAY_7 -> {
                    setColoredPercentageValue(weekCurrency, currencyValue)

                    if (ethValue == null) hideEthColumn()
                    else setColoredPercentageValue(weekEth, ethValue)

                    if (btcValue == null) hideBtcColumn()
                    else setColoredPercentageValue(weekBtc, btcValue)
                }
                TimePeriod.DAY_30 -> {
                    setColoredPercentageValue(monthCurrency, currencyValue)

                    if (ethValue == null) hideEthColumn()
                    else setColoredPercentageValue(monthEth, ethValue)

                    if (btcValue == null) hideBtcColumn()
                    else setColoredPercentageValue(monthBtc, btcValue)
                }
            }
        }
    }

    private fun hideEthColumn() {
        ethTitle.isVisible = false
        ethTitleLine.isVisible = false
        weekEth.isVisible = false
        weekEthLine.isVisible = false
        monthEth.isVisible = false
        monthEthLine.isVisible = false
    }

    private fun hideBtcColumn() {
        btcTitle.isVisible = false
        btcTitleLine.isVisible = false
        weekBtc.isVisible = false
        weekBtcLine.isVisible = false
        monthBtc.isVisible = false
        monthBtcLine.isVisible = false
    }

    private fun setColoredPercentageValue(textView: TextView, percentage: BigDecimal) {
        val color = if (percentage >= BigDecimal.ZERO) R.color.remus else R.color.lucian
        val sign = if (percentage >= BigDecimal.ZERO) "+" else "-"

        textView.setTextColor(context.getColor(color))
        textView.text = App.numberFormatter.format(percentage.abs(), 0, 2, prefix = sign, suffix = "%")
    }

}
