package io.horizontalsystems.chartview.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.horizontalsystems.chartview.R
import kotlinx.android.synthetic.main.chart_indicator_view.view.*

class ChartIndicatorView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.chart_indicator_view, this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ChartIndicatorView)
        try {
            setTitle(attributes.getString(R.styleable.ChartIndicatorView_title))
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
        stateIcon.setImageResource(if (enabled) R.drawable.ic_eye_enabled_20px else R.drawable.ic_eye_disabled_20px)
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

enum class ChartInfoTrend {
    UP, DOWN, NEUTRAL
}
