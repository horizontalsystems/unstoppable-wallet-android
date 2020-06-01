package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.ratechart.ChartInfoTrend
import kotlinx.android.synthetic.main.chart_indicator_view.view.*

class ChartIndicatorView : LinearLayout {

    init {
        inflate(context, R.layout.chart_indicator_view, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    fun bind(title: String, enabled: Boolean, trend: ChartInfoTrend) {
        stateIcon.setImageResource(if (enabled) R.drawable.ic_hide_16 else R.drawable.ic_show_16)
        titleText.text = title

        trendText.setText(getTrendText(trend))
        trendText.setTextColor(ContextCompat.getColor(context, getTrendTextColor(trend)))
    }

    private fun getTrendText(trend: ChartInfoTrend): Int {
        return when(trend){
            ChartInfoTrend.DOWN -> R.string.Charts_Trend_Down
            ChartInfoTrend.UP -> R.string.Charts_Trend_Up
            ChartInfoTrend.NEUTRAL -> R.string.Charts_Trend_Neutral
        }
    }

    private fun getTrendTextColor(trend: ChartInfoTrend): Int {
        return when(trend){
            ChartInfoTrend.DOWN -> R.color.red_d
            ChartInfoTrend.UP -> R.color.green_d
            ChartInfoTrend.NEUTRAL -> R.color.grey
        }
    }

}
