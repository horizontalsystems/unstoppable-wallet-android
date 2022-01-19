package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.databinding.ViewMarketMetricSmallBinding
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.chartview.ChartData
import java.math.BigDecimal

class MarketMetricSmallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewMarketMetricSmallBinding.inflate(LayoutInflater.from(context), this)

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.MarketMetricSmallView)
        try {
            binding.title.text = ta.getString(R.styleable.MarketMetricSmallView_title)
            binding.value.text = ta.getString(R.styleable.MarketMetricSmallView_value)
        } finally {
            ta.recycle()
        }
    }

    fun setMetricData(data: MetricData) {
        binding.title.text = context.getString(data.type.title)
        binding.title.isVisible = true
        setValueText(data.value, true)
        setDiff(data.diff, true)

        data.chartData?.let {
            post {
                binding.chart.setData(it)
            }
        }
    }

    private fun setValueText(valueText: String?, modeNotAvailable: Boolean) {
        when {
            valueText != null -> {
                binding.value.text = valueText
                binding.value.setTextColor(context.getColor(R.color.bran))
            }
            modeNotAvailable -> {
                binding.value.text = context.getString(R.string.NotAvailable)
                binding.value.setTextColor(context.getColor(R.color.grey_50))
            }
            else -> {
                binding.value.text = null
            }
        }
    }

    private fun setDiff(diff: BigDecimal?, modeNotAvailable: Boolean) {
        when {
            diff != null -> {
                val sign = if (diff >= BigDecimal.ZERO) "+" else "-"
                binding.diffPercentage.text =
                    App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")
                val textColor = if (diff >= BigDecimal.ZERO) R.color.remus else R.color.lucian
                binding.diffPercentage.setTextColor(context.getColor(textColor))
            }
            modeNotAvailable -> {
                binding.diffPercentage.text = "----"
                binding.diffPercentage.setTextColor(context.getColor(R.color.grey_50))
            }
            else -> {
                binding.diffPercentage.text = null
            }
        }
    }
}

data class MetricData(
    val value: String?,
    val diff: BigDecimal?,
    val chartData: ChartData?,
    val type: MetricsType
)
