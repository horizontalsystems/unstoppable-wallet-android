package io.horizontalsystems.chartview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.chartview.Indicator.Candle
import io.horizontalsystems.chartview.databinding.ViewChartMinimalBinding
import io.horizontalsystems.chartview.models.ChartConfig

class ChartMinimal @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewChartMinimalBinding.inflate(LayoutInflater.from(context), this)

    private val config = ChartConfig(context, attrs)

    private val mainCurve = ChartCurve2(config)
    private val mainGradient = ChartGradient()

    fun setData(data: ChartData) {
        config.setTrendColor(data)

        val candleValues = data.valuesByTimestamp(Candle)
        val minCandleValue = candleValues.values.minOrNull() ?: 0f
        val maxCandleValue = candleValues.values.maxOrNull() ?: 0f

        val mainCurveAnimator = CurveAnimator(
            candleValues,
            data.startTimestamp,
            data.endTimestamp,
            minCandleValue,
            maxCandleValue,
            null,
            binding.chartMain.shape.right,
            binding.chartMain.shape.bottom,
            0f,
            config.curveMinimalVerticalOffset,
        )
        mainCurveAnimator.nextFrame(1f)

        mainCurve.setShape(binding.chartMain.shape)
        mainCurve.setCurveAnimator(mainCurveAnimator)
        mainCurve.setColor(config.curveColor)

        mainGradient.setCurveAnimator(mainCurveAnimator)
        mainGradient.setShape(binding.chartMain.shape)
        mainGradient.setShader(config.curveGradient)

        binding.chartMain.clear()
        binding.chartMain.add(mainCurve, mainGradient)

        binding.chartMain.invalidate()
    }

}
