package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import kotlinx.android.synthetic.main.view_market_metric_large.view.*
import java.math.BigDecimal

class MarketMetricLargeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_market_metric_large, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.MarketMetricLargeView)
        try {
            setTitle(ta.getString(R.styleable.MarketMetricLargeView_title))
            setValue(ta.getString(R.styleable.MarketMetricLargeView_value))
        } finally {
            ta.recycle()
        }
    }

    fun setValue(v: String?) {
        value.text = v
    }

    fun setTitle(v: String?) {
        title.text = v
    }

    fun setMetricData(data: MetricData?) {
        setValue(data?.value)
        setDiff(data?.diff)
    }

    fun setDiff(v: BigDecimal?) {
        diffCircle.animateHorizontal(v?.toFloat())
        if (v == null) return

        val sign = if (v >= BigDecimal.ZERO) "+" else "-"
        val textColor = if (v >= BigDecimal.ZERO) R.color.green_d else R.color.red_d

        diffPercentage.setTextColor(context.getColor(textColor))
        diffPercentage.text = App.numberFormatter.format(v.abs(), 0, 2, sign, "%")
    }
}

data class MetricData(val value: String, val diff: BigDecimal)
