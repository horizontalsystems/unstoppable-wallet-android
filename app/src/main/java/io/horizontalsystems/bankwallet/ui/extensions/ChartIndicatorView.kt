package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.ratechart.ChartInfoTrend
import kotlinx.android.synthetic.main.chart_indicator_view.view.*

class ChartIndicatorView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    init {
        inflate(context, R.layout.chart_indicator_view, this)

        val attributes = context.obtainStyledAttributes(attrs, io.horizontalsystems.views.R.styleable.ChartIndicatorView)
        try {
            setTitle(attributes.getString(io.horizontalsystems.views.R.styleable.ChartIndicatorView_title))
        }finally {
            attributes.recycle()
        }
    }

    private fun setTitle(titleText: String?) {
        title.text = titleText
    }

    fun bind(trend: ChartInfoTrend? = null) {
        trend?.let {
            trendText.setText(getTrendText(it))
            trendText.setTextColor(ContextCompat.getColor(context, getTrendTextColor(it)))
        }
    }

    fun setStateEnabled(enabled: Boolean){
        stateIcon.setImageResource(if (enabled) R.drawable.ic_hide_16 else R.drawable.ic_show_16)
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
