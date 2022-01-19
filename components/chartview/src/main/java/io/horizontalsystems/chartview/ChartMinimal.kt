package io.horizontalsystems.chartview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.chartview.Indicator.Candle
import io.horizontalsystems.chartview.databinding.ViewChartMinimalBinding
import io.horizontalsystems.chartview.helpers.PointConverter
import io.horizontalsystems.chartview.models.ChartConfig

class ChartMinimal @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewChartMinimalBinding.inflate(LayoutInflater.from(context), this)

    private val config = ChartConfig(context, attrs)

    private val mainCurve = ChartCurve(config, isVisible = true)
    private val mainGradient = ChartGradient()


    fun setData(data: ChartData) {
        config.setTrendColor(data)

        val points = PointConverter.curveForMinimal(
            data.values(Candle),
            binding.chartMain.shape,
            config.curveMinimalVerticalOffset
        )

        mainCurve.setShape(binding.chartMain.shape)
        mainCurve.setPoints(points)
        mainCurve.setColor(config.curveColor)

        mainGradient.setPoints(points)
        mainGradient.setShape(binding.chartMain.shape)
        mainGradient.setShader(config.curveGradient)

        binding.chartMain.clear()
        binding.chartMain.add(mainCurve, mainGradient)

        binding.chartMain.invalidate()
    }

}
