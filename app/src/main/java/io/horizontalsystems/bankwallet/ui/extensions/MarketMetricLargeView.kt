package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
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
        diffCircle.post {
            diffCircle.animateHorizontal(v?.let { it.toFloat() * 3 })
        }
        if (v == null) return
        title.isVisible = true

        val sign = if (v >= BigDecimal.ZERO) "+" else "-"
        diffPercentage.text = App.numberFormatter.format(v.abs(), 0, 2, sign, "%")

        val color = if (v >= BigDecimal.ZERO) R.color.remus else R.color.lucian
        diffPercentage.setTextColor(context.getColor(color))
    }
}

data class MetricData(val value: String?, val diff: BigDecimal?)
