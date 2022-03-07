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

    private val mainCurve = ChartCurveXxx(config)
    private val mainGradient = ChartGradient()

    private var mainCurveZzz: Zzz? = null

    fun setData(data: ChartData) {
        config.setTrendColor(data)

        mainCurveZzz = Zzz(
            data.valuesByTimestamp(Candle),
            data.startTimestamp,
            data.endTimestamp,
            mainCurveZzz,
            binding.chartMain.shape.right,
            binding.chartMain.shape.bottom,
            config.curveVerticalOffset
        )

        mainCurve.setShape(binding.chartMain.shape)
        mainCurve.setZzz(mainCurveZzz!!)
        mainCurve.setColor(config.curveColor)

        mainGradient.setZzz(mainCurveZzz!!)
        mainGradient.setShape(binding.chartMain.shape)
        mainGradient.setShader(config.curveGradient)

        binding.chartMain.clear()
        binding.chartMain.add(mainCurve, mainGradient)

        binding.chartMain.invalidate()
    }

}
