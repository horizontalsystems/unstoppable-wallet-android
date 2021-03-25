package io.horizontalsystems.bankwallet.modules.coin

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.xrateskit.entities.TimePeriod
import kotlinx.android.synthetic.main.view_coin_performance_row.view.*
import java.math.BigDecimal

class CoinPerformanceRowView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_coin_performance_row, this)
        orientation = HORIZONTAL
    }

    fun bindHeader(roiText: String, fiatText: String, btcText: String?, ethText: String?) {
        headerTextView.text = roiText
        headerTextView.setTextAppearance(R.style.Subhead1)

        fiatTextView.text = fiatText

        if (btcText == null) hideBtcColumn()
        else btcTextView.text = btcText

        if (ethText == null) hideEthColumn()
        else ethTextView.text = ethText

        setBackgroundResource(R.drawable.rounded_lawrence_background_top)
    }

    fun bind(period: TimePeriod, fiatDiff: BigDecimal, btcDiff: BigDecimal?, ethDiff: BigDecimal?, background: Int) {
        headerTextView.text = getHeaderText(period)
        headerTextView.setTextColor(context.getColor(R.color.grey))

        setColoredPercentageValue(fiatTextView, fiatDiff)

        if (btcDiff == null) hideBtcColumn()
        else setColoredPercentageValue(btcTextView, btcDiff)

        if (ethDiff == null) hideEthColumn()
        else setColoredPercentageValue(ethTextView, ethDiff)

        setBackgroundResource(background)
    }

    private fun hideBtcColumn() {
        btcTextView.isVisible = false
        btcLine.isVisible = false
    }

    private fun hideEthColumn() {
        ethTextView.isVisible = false
        ethLine.isVisible = false
    }

    private fun setColoredPercentageValue(textView: TextView, percentage: BigDecimal) {
        val color = if (percentage >= BigDecimal.ZERO) R.color.remus else R.color.lucian
        val sign = if (percentage >= BigDecimal.ZERO) "+" else "-"

        textView.setTextColor(context.getColor(color))
        textView.text = App.numberFormatter.format(percentage.abs(), 0, 2, prefix = sign, suffix = "%")
    }

    private fun getHeaderText(period: TimePeriod): String {
        return when (period) {
            TimePeriod.DAY_7 -> context.getString(R.string.CoinPage_Performance_Week)
            TimePeriod.DAY_30 -> context.getString(R.string.CoinPage_Performance_Month)
            else -> ""
        }
    }

    companion object{
        fun getBackground(size: Int, position: Int): Int {
            return when (position) {
                size - 1 -> R.drawable.rounded_steel10_background_bottom
                else -> R.drawable.rounded_steel10_background_middle
            }
        }
    }

}
