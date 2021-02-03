package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_market_metric_small.view.*
import java.math.BigDecimal

class MarketMetricSmallView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_market_metric_small, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.MarketMetricSmallView)
        try {
            title.text = ta.getString(R.styleable.MarketMetricSmallView_title)
            value.text = ta.getString(R.styleable.MarketMetricSmallView_value)
        } finally {
            ta.recycle()
        }
    }

    fun setMetricData(data: MetricData?) {
        if (data == null) {
            title.visibility = View.INVISIBLE
            setValueText(null, false)
            setDiff(null, false)
        } else {
            title.visibility = View.VISIBLE
            setValueText(data.value, true)
            setDiff(data.diff, true)
        }
    }

    private fun setValueText(valueText: String?, modeNotAvailable: Boolean) {
        when {
            valueText != null -> {
                value.text = valueText
                LayoutHelper.getAttr(R.attr.ColorBran, context.theme)?.let { value.setTextColor(it) }
            }
            modeNotAvailable -> {
                value.text = context.getString(R.string.NotAvailable)
                value.setTextColor(context.getColor(R.color.grey_50))
            }
            else -> {
                value.text = null
            }
        }
    }

    private fun setDiff(diff: BigDecimal?, modeNotAvailable: Boolean) {
        when {
            diff != null -> {
                diffCircle.animateVertical(diff.toFloat())

                val sign = if (diff >= BigDecimal.ZERO) "+" else "-"
                diffPercentage.text = App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")
                val textColor = if (diff >= BigDecimal.ZERO) R.attr.ColorRemus else R.attr.ColorLucian
                LayoutHelper.getAttr(textColor, context.theme)?.let {
                    diffPercentage.setTextColor(it)
                }
            }
            modeNotAvailable -> {
                diffCircle.setBackground(LayoutHelper.getAttr(R.attr.ColorJeremy, context.theme, R.color.grey))
                diffPercentage.text = "----"
                diffPercentage.setTextColor(context.getColor(R.color.grey_50))
            }
            else -> {
                diffCircle.animateVertical(null)
                diffPercentage.text = null
            }
        }
    }
}

